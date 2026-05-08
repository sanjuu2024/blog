package com.ccsanjuu.blog.common.exception;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.common.api.ResultCode;
import jakarta.servlet.ServletException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 全局 REST 异常处理器。
 *
 * <p>本类负责把 Controller、参数绑定、参数校验、数据库唯一索引冲突等异常统一转换为
 * 项目约定的 {@link Result} 响应结构，避免接口直接暴露 Spring、数据库或底层框架的异常细节。</p>
 *
 * <p>处理策略按“越明确越靠前”的顺序组织：</p>
 * <ul>
 *     <li>业务异常：直接使用业务层抛出的 {@link ResultCode}。</li>
 *     <li>参数异常：统一返回 {@link ResultCode#PARAM_INVALID}，并附带字段级错误列表。</li>
 *     <li>唯一索引冲突：根据约定索引名映射到用户名/邮箱等更具体的业务错误码。</li>
 *     <li>资源不存在与未分类异常：分别兜底为 404 和系统内部异常。</li>
 * </ul>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理业务层主动抛出的异常。
     *
     * <p>{@link BizException} 通常表示业务规则已经明确失败，例如用户名已存在、用户被禁用等。
     * 这类异常不需要再包装成系统错误，而是直接透传对应的业务状态码和 HTTP 状态码。</p>
     *
     * @param ex 业务异常，内部携带项目统一的 {@link ResultCode}
     * @return 符合统一响应结构的错误响应
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException ex) {
        String message = StringUtils.hasText(ex.getMessage())
                ? ex.getMessage()
                : ex.getResultCode().getMessage();
        log.warn("业务异常 code={}, message={}", ex.getResultCode().getCode(), message);
        return buildResponse(ex.getResultCode(), null, message);
    }

    /**
     * 处理 Bean Validation 触发的参数校验异常。
     *
     * <p>覆盖请求体 DTO 校验、查询参数/路径参数校验，以及 Spring 6.1+ 方法参数校验异常。
     * 所有校验失败都收敛为字段级错误列表，便于前端按字段展示提示。</p>
     *
     * @param ex 参数校验相关异常
     * @return {@code 199001 请求参数不合法} 响应，data 为字段错误列表
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<Result<List<FieldValidationError>>> handleValidationException(Exception ex) {
        List<FieldValidationError> fieldErrors = resolveFieldErrors(ex);
        log.warn("参数校验失败 errors={}", fieldErrors);
        return buildParamInvalidResponse(fieldErrors);
    }

    /**
     * 处理请求解析阶段的异常。
     *
     * <p>这类异常通常发生在进入 Controller 方法之前，例如 JSON 格式错误、缺少必填 query 参数、
     * query/path 参数类型不匹配，或 Servlet 层抛出的请求异常。它们同样归类为参数不合法。</p>
     *
     * @param ex 请求解析相关异常
     * @return {@code 199001 请求参数不合法} 响应，data 为字段错误列表
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            ServletException.class
    })
    public ResponseEntity<Result<List<FieldValidationError>>> handleRequestException(Exception ex) {
        List<FieldValidationError> fieldErrors = resolveRequestErrors(ex);
        log.warn("请求参数解析失败 errors={}", fieldErrors);
        return buildParamInvalidResponse(fieldErrors);
    }

    /**
     * 处理数据库唯一索引冲突。
     *
     * <p>service 层会主动做唯一性校验，但并发情况下仍可能由数据库唯一索引兜底拦截。
     * 这里根据 migration 中约定的索引名，把底层 {@link DuplicateKeyException} 转换为更准确的业务错误。</p>
     *
     * @param ex 唯一索引冲突异常
     * @return 用户名已存在、邮箱已存在或通用参数错误响应
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKeyException(DuplicateKeyException ex) {
        ResultCode resultCode = resolveDuplicateKeyResultCode(ex);
        log.warn("唯一索引冲突 code={}, message={}", resultCode.getCode(), resolveThrowableMessages(ex));
        return buildResponse(resultCode, null, null);
    }

    /**
     * 处理不存在的静态资源或未匹配路由。
     *
     * <p>Spring MVC 在找不到资源时可能抛出 {@link NoResourceFoundException}。统一转换为项目约定的
     * {@link ResultCode#RESOURCE_NOT_FOUND}，避免返回框架默认错误页或默认 JSON。</p>
     *
     * @param ex 资源不存在异常
     * @return 统一的资源不存在响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("请求资源不存在 path={}", ex.getResourcePath());
        return buildResponse(ResultCode.RESOURCE_NOT_FOUND, null, null);
    }

    /**
     * 最后的兜底异常处理。
     *
     * <p>没有被前面明确捕获的异常都会进入这里。日志保留完整堆栈，响应只返回统一系统错误，
     * 避免把内部实现、SQL、堆栈等敏感信息暴露给客户端。</p>
     *
     * @param ex 未分类异常
     * @return 统一的系统内部异常响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("系统错误", ex);
        return buildResponse(ResultCode.SYSTEM_ERROR, null, null);
    }

    /**
     * 将不同来源的 Bean Validation 异常解析成统一字段错误列表。
     *
     * <p>Spring 在不同参数位置使用不同异常类型：请求体 DTO 常见为
     * {@link MethodArgumentNotValidException}，表单/查询对象绑定常见为 {@link BindException}，
     * 单个参数约束可能是 {@link ConstraintViolationException} 或
     * {@link HandlerMethodValidationException}。这里把差异抹平，供响应构造方法统一使用。</p>
     *
     * @param ex 参数校验异常
     * @return 字段级校验错误列表；无法识别时返回空列表
     */
    private List<FieldValidationError> resolveFieldErrors(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            return resolveBindingResultErrors(methodArgumentNotValidException.getBindingResult());
        }
        if (ex instanceof BindException bindException) {
            return resolveBindingResultErrors(bindException.getBindingResult());
        }
        if (ex instanceof ConstraintViolationException constraintViolationException) {
            return constraintViolationException.getConstraintViolations().stream()
                    .map(violation -> new FieldValidationError(
                            extractLastPathNode(violation.getPropertyPath().toString()),
                            violation.getMessage(),
                            violation.getInvalidValue()))
                    .toList();
        }
        if (ex instanceof HandlerMethodValidationException handlerMethodValidationException) {
            return resolveHandlerMethodValidationErrors(handlerMethodValidationException);
        }
        return List.of();
    }

    /**
     * 将请求解析阶段异常转换为统一字段错误列表。
     *
     * <p>这些异常往往没有标准的 {@link Errors} 结构，因此需要手动指定 field、message 和 rejectedValue。
     * 例如 JSON 解析失败统一落在 {@code requestBody} 字段，便于前端识别这是请求体整体格式问题。</p>
     *
     * @param ex 请求解析异常
     * @return 字段级错误列表
     */
    private List<FieldValidationError> resolveRequestErrors(Exception ex) {
        if (ex instanceof HttpMessageNotReadableException httpMessageNotReadableException) {
            return List.of(new FieldValidationError(
                    "requestBody",
                    "请求体格式错误或字段类型不匹配",
                    resolveMostSpecificCauseMessage(httpMessageNotReadableException)
            ));
        }
        if (ex instanceof MissingServletRequestParameterException missingServletRequestParameterException) {
            return List.of(new FieldValidationError(
                    missingServletRequestParameterException.getParameterName(),
                    "缺少必填请求参数",
                    null
            ));
        }
        if (ex instanceof MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {
            return List.of(new FieldValidationError(
                    methodArgumentTypeMismatchException.getName(),
                    "请求参数类型不匹配",
                    methodArgumentTypeMismatchException.getValue()
            ));
        }
        if (ex instanceof ServletException servletException && StringUtils.hasText(servletException.getMessage())) {
            return List.of(new FieldValidationError(
                    "request",
                    servletException.getMessage(),
                    null
            ));
        }
        return List.of(new FieldValidationError("request", ResultCode.PARAM_INVALID.getMessage(), null));
    }

    /**
     * 从 Spring 的绑定结果中提取字段错误和对象级错误。
     *
     * <p>字段错误对应具体字段，例如 {@code username} 长度不合法；对象级错误通常来自跨字段校验，
     * 没有明确字段时使用对象名承载，避免错误信息丢失。</p>
     *
     * @param errorsSource Spring 参数绑定/校验错误集合
     * @return 项目统一的字段错误列表
     */
    private List<FieldValidationError> resolveBindingResultErrors(Errors errorsSource) {
        List<FieldValidationError> errors = new ArrayList<>();

        for (FieldError fieldError : errorsSource.getFieldErrors()) {
            errors.add(new FieldValidationError(
                    fieldError.getField(),
                    fieldError.getDefaultMessage(),
                    fieldError.getRejectedValue()));
        }

        for (ObjectError globalError : errorsSource.getGlobalErrors()) {
            errors.add(new FieldValidationError(
                    globalError.getObjectName(),
                    globalError.getDefaultMessage(),
                    null));
        }

        return errors;
    }

    /**
     * 解析 Spring 6.1+ 的方法参数校验异常。
     *
     * <p>{@link HandlerMethodValidationException} 会按方法参数聚合错误。如果参数本身可转为
     * {@link ParameterErrors}，说明它仍包含标准绑定结果；否则就从参数名、被拒绝值和 resolvable error
     * 中手动组装字段错误。</p>
     *
     * @param ex 方法参数校验异常
     * @return 项目统一的字段错误列表
     */
    private List<FieldValidationError> resolveHandlerMethodValidationErrors(
            HandlerMethodValidationException ex
    ) {
        List<FieldValidationError> errors = new ArrayList<>();

        for (ParameterValidationResult validationResult : ex.getParameterValidationResults()) {
            if (validationResult instanceof ParameterErrors parameterErrors) {
                errors.addAll(resolveBindingResultErrors(parameterErrors));
                continue;
            }

            String parameterName = validationResult.getMethodParameter().getParameterName();
            Object rejectedValue = validationResult.getArgument();

            for (MessageSourceResolvable resolvable : validationResult.getResolvableErrors()) {
                errors.add(new FieldValidationError(
                        StringUtils.hasText(parameterName) ? parameterName : "unknown",
                        resolvable.getDefaultMessage(),
                        rejectedValue
                ));
            }
        }

        return errors;
    }

    /**
     * 构造参数不合法响应。
     *
     * <p>所有参数类错误都固定使用 {@link ResultCode#PARAM_INVALID}，但 data 中保留字段错误列表，
     * 这样既满足统一错误码，又能给前端足够细的表单提示信息。</p>
     *
     * @param fieldErrors 字段级错误列表
     * @return HTTP 400 + 统一响应体
     */
    private ResponseEntity<Result<List<FieldValidationError>>> buildParamInvalidResponse(
            List<FieldValidationError> fieldErrors
    ) {
        Result<List<FieldValidationError>> body = Result.<List<FieldValidationError>>builder()
                .code(ResultCode.PARAM_INVALID.getCode())
                .message(ResultCode.PARAM_INVALID.getMessage())
                .data(fieldErrors)
                .build();
        return ResponseEntity.status(HttpStatusCode.valueOf(ResultCode.PARAM_INVALID.getHttpStatus()))
                .body(body);
    }

    /**
     * 构造通用错误响应。
     *
     * <p>HTTP 状态码来自 {@link ResultCode#getHttpStatus()}，业务状态码和默认消息来自
     * {@link ResultCode} 本身；当调用方提供 customMessage 时，用它覆盖默认消息。</p>
     *
     * @param resultCode 项目统一业务状态码
     * @param data 响应数据，错误场景通常为空
     * @param customMessage 可选自定义消息
     * @param <T> 响应数据类型
     * @return 带 HTTP 状态码的统一响应体
     */
    private <T> ResponseEntity<Result<T>> buildResponse(ResultCode resultCode, T data, String customMessage) {
        Result<T> body = Result.<T>builder()
                .code(resultCode.getCode())
                .message(StringUtils.hasText(customMessage) ? customMessage : resultCode.getMessage())
                .data(data)
                .build();
        return ResponseEntity.status(HttpStatusCode.valueOf(resultCode.getHttpStatus()))
                .body(body);
    }

    /**
     * 从 Bean Validation 的属性路径中取最后一段作为字段名。
     *
     * <p>例如 {@code register.request.username} 会转换为 {@code username}。这样前端不需要理解
     * Java 方法参数路径，只关注真实表单字段即可。</p>
     *
     * @param path 校验框架返回的属性路径
     * @return 最末级字段名；空路径时返回 {@code unknown}
     */
    private String extractLastPathNode(String path) {
        if (!StringUtils.hasText(path)) {
            return "unknown";
        }
        int lastDotIndex = path.lastIndexOf('.');
        return lastDotIndex >= 0 ? path.substring(lastDotIndex + 1) : path;
    }

    /**
     * 获取 JSON 解析异常中最具体的原因信息。
     *
     * <p>Jackson 的原始异常往往包含更准确的字段类型、格式或枚举值错误信息；
     * 如果没有更深层原因，则退回使用 Spring 包装异常本身的消息。</p>
     *
     * @param ex 请求体不可读异常
     * @return 最具体的异常消息
     */
    private String resolveMostSpecificCauseMessage(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        return cause != null ? cause.getMessage() : ex.getMessage();
    }

    /**
     * 根据数据库唯一索引名推断业务错误码。
     *
     * <p>这里依赖 Flyway migration 中的索引命名约定。若后续新增唯一索引，也应在这里补充映射，
     * 否则会按通用 {@link ResultCode#PARAM_INVALID} 处理。</p>
     *
     * @param ex 数据库唯一索引冲突异常
     * @return 更具体的业务状态码
     */
    private ResultCode resolveDuplicateKeyResultCode(Throwable ex) {
        String message = resolveThrowableMessages(ex).toLowerCase(Locale.ROOT);
        if (message.contains("uq_blog_user_username_lower")) {
            return ResultCode.USERNAME_EXISTS;
        }
        if (message.contains("uq_blog_user_email_lower")) {
            return ResultCode.EMAIL_EXISTS;
        }
        return ResultCode.PARAM_INVALID;
    }

    /**
     * 汇总异常链路上的所有 message。
     *
     * <p>数据库驱动或 Spring 包装异常时，关键信息可能在 cause 链的任意一层。把整条链拼接后，
     * 可以提高索引名匹配和日志排查的稳定性。</p>
     *
     * @param ex 根异常
     * @return 使用 {@code |} 拼接的异常消息链
     */
    private String resolveThrowableMessages(Throwable ex) {
        List<String> messages = new ArrayList<>();
        Throwable current = ex;
        while (current != null) {
            if (StringUtils.hasText(current.getMessage())) {
                messages.add(current.getMessage());
            }
            current = current.getCause();
        }
        return String.join(" | ", messages);
    }

    /**
     * 字段级参数错误。
     *
     * @param field 发生错误的字段名；无法定位时使用 {@code request}、{@code requestBody} 或 {@code unknown}
     * @param message 面向调用方的错误说明
     * @param rejectedValue 被拒绝的原始值；无法或不适合暴露时为空
     */
    public record FieldValidationError(String field, String message, Object rejectedValue) {
    }
}
