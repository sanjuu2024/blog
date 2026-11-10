package com.ccsanjuu.blog.modules.audit.aspect;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.audit.annotation.AdminAudit;
import com.ccsanjuu.blog.modules.audit.model.bo.AdminAuditRecordBO;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResult;
import com.ccsanjuu.blog.modules.audit.service.AdminAuditLogService;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 统一记录带有 {@link AdminAudit} 注解的后台管理操作。
 *
 * <p>切面使用同一个事务包住 Controller 写操作和 SUCCESS 审计，任意一方失败都会一起回滚。
 * 失败审计则在业务事务回滚后使用独立事务写入，既保证成功记录的一致性，也保留失败尝试。</p>
 */
@Slf4j
@Aspect
@Component
public class AdminAuditAspect {

    private static final int RESOURCE_ID_MAX_LENGTH = 4000;
    private static final int DETAIL_MAX_LENGTH = 255;
    private static final int FAILURE_MESSAGE_MAX_LENGTH = 255;
    private static final int REQUEST_PATH_MAX_LENGTH = 500;

    private final AdminAuditLogService adminAuditLogService;
    private final TransactionTemplate transactionTemplate;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public AdminAuditAspect(
            AdminAuditLogService adminAuditLogService,
            PlatformTransactionManager transactionManager
    ) {
        this.adminAuditLogService = adminAuditLogService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 执行后台操作并按实际结果追加审计记录。
     *
     * @param joinPoint 被拦截的方法
     * @param audit 审计配置
     * @return 原方法返回值
     * @throws Throwable 原方法抛出的异常
     */
    @Around("@annotation(audit)")
    public Object recordAudit(ProceedingJoinPoint joinPoint, AdminAudit audit) throws Throwable {
        Object[] resultHolder = new Object[1];
        try {
            return transactionTemplate.execute(status -> {
                try {
                    Object result = joinPoint.proceed();
                    resultHolder[0] = result;
                    AdminAuditRecordBO record = buildAuditRecord(joinPoint, audit, result, null);
                    if (record != null) {
                        adminAuditLogService.recordSuccess(record);
                    }
                    return result;
                } catch (Throwable error) {
                    throw new AuditInvocationException(error);
                }
            });
        } catch (AuditInvocationException exception) {
            saveFailure(joinPoint, audit, resultHolder[0], exception.getOriginalError());
            throw exception.getOriginalError();
        } catch (Throwable error) {
            // 包含事务提交失败等发生在原方法返回之后的异常。
            saveFailure(joinPoint, audit, resultHolder[0], error);
            throw error;
        }
    }

    /**
     * 组装一条不包含请求体和查询参数的审计记录。
     *
     * @param joinPoint 被拦截的方法
     * @param audit 审计配置
     * @param result 原方法返回值，失败时为空
     * @param error 原方法异常，成功时为空
     */
    private AdminAuditRecordBO buildAuditRecord(
            ProceedingJoinPoint joinPoint,
            AdminAudit audit,
            Object result,
            Throwable error
    ) {
        JwtPrincipal principal = getPrincipal();
        if (principal == null) {
            log.error("audit_event=SKIPPED reason=ADMIN_PRINCIPAL_MISSING action={}", audit.action());
            return null;
        }

        HttpServletRequest request = getRequest();
        Integer failureCode = null;
        String failureMessage = null;
        if (error instanceof BizException bizException) {
            failureCode = bizException.getResultCode().getCode();
            String message = StringUtils.hasText(bizException.getMessage())
                    ? bizException.getMessage()
                    : bizException.getResultCode().getMessage();
            failureMessage = limit(message, FAILURE_MESSAGE_MAX_LENGTH);
        } else if (error != null) {
            failureCode = ResultCode.SYSTEM_ERROR.getCode();
            failureMessage = ResultCode.SYSTEM_ERROR.getMessage();
        }

        return new AdminAuditRecordBO(
                principal.userId(),
                principal.username(),
                audit.resourceType(),
                evaluate(audit.resourceId(), joinPoint, result, RESOURCE_ID_MAX_LENGTH),
                audit.action(),
                evaluate(audit.detail(), joinPoint, result, DETAIL_MAX_LENGTH),
                error == null ? AdminAuditResult.SUCCESS : AdminAuditResult.FAILURE,
                failureCode,
                failureMessage,
                request == null ? "" : request.getMethod(),
                request == null ? "" : limit(request.getRequestURI(), REQUEST_PATH_MAX_LENGTH)
        );
    }

    /**
     * 在原业务事务结束后保存失败审计；记录失败不能覆盖原始业务异常。
     *
     * @param joinPoint 被拦截的方法
     * @param audit 审计配置
     * @param result 原方法返回值，提交失败时可能非空
     * @param error 原始业务或事务异常
     */
    private void saveFailure(ProceedingJoinPoint joinPoint, AdminAudit audit, Object result, Throwable error) {
        AdminAuditRecordBO record = buildAuditRecord(joinPoint, audit, result, error);
        if (record == null) {
            return;
        }
        try {
            adminAuditLogService.recordFailure(record);
        } catch (Exception auditError) {
            log.error(
                    "audit_event=PERSIST_FAILED operatorId={} action={} resourceType={}",
                    record.operatorId(),
                    audit.action(),
                    audit.resourceType(),
                    auditError
            );
        }
    }

    /**
     * 从当前 Spring Security 上下文获取管理员身份快照。
     *
     * @return 当前管理员；身份不存在时返回 {@code null}
     */
    private JwtPrincipal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof JwtPrincipal principal
                ? principal
                : null;
    }

    /**
     * 获取当前 HTTP 请求，仅用于记录方法和不含 query 的路径。
     *
     * @return 当前请求；非 Web 调用时返回 {@code null}
     */
    private HttpServletRequest getRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }

    /**
     * 计算注解中明确声明的 Spring EL，并把集合统一转换为逗号分隔的资源标识。
     *
     * @param expression Spring EL
     * @param joinPoint 被拦截的方法
     * @param result 原方法返回值
     * @param maxLength 最大保存长度
     * @return 表达式结果；未配置或结果为空时返回 {@code null}
     */
    private String evaluate(String expression, ProceedingJoinPoint joinPoint, Object result, int maxLength) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                joinPoint.getTarget(),
                method,
                joinPoint.getArgs(),
                parameterNameDiscoverer
        );
        context.setVariable("result", result);
        Object value;
        try {
            value = expressionParser.parseExpression(expression).getValue(context);
        } catch (EvaluationException ignored) {
            // 创建类操作失败时没有 result，目标 ID 无法取得，但失败操作本身仍应正常记录。
            return null;
        }
        if (value == null) {
            return null;
        }
        String text = value instanceof Collection<?> collection
                ? collection.stream().map(String::valueOf).collect(Collectors.joining(","))
                : String.valueOf(value);
        return limit(text, maxLength);
    }

    /**
     * 截断即将落库的外部文本，避免异常消息或请求路径超过表字段限制。
     *
     * @param value 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 让任意原始 Throwable 可以穿过 TransactionTemplate 的 RuntimeException 回调边界。
     */
    private static class AuditInvocationException extends RuntimeException {

        private final Throwable originalError;

        AuditInvocationException(Throwable originalError) {
            super(originalError);
            this.originalError = originalError;
        }

        Throwable getOriginalError() {
            return originalError;
        }
    }
}
