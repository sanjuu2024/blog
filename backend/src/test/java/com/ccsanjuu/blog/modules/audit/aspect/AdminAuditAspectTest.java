package com.ccsanjuu.blog.modules.audit.aspect;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.audit.annotation.AdminAudit;
import com.ccsanjuu.blog.modules.audit.model.bo.AdminAuditRecordBO;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResult;
import com.ccsanjuu.blog.modules.audit.service.AdminAuditLogService;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAuditAspectTest {

    private AdminAuditLogService auditLogService;
    private AuditedTarget target;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AdminAuditLogService.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new AuditedTarget());
        proxyFactory.addAspect(new AdminAuditAspect(auditLogService, transactionManager));
        target = proxyFactory.getProxy();

        JwtPrincipal principal = new JwtPrincipal(10001L, "admin", "ADMIN", "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/api/v1/admin/articles")
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldRecordSuccessfulOperationAndResolveExpressions() {
        assertEquals(40001L, target.create(40001L));

        ArgumentCaptor<AdminAuditRecordBO> captor = ArgumentCaptor.forClass(AdminAuditRecordBO.class);
        verify(auditLogService).recordSuccess(captor.capture());
        AdminAuditRecordBO record = captor.getValue();
        assertEquals(10001L, record.operatorId());
        assertEquals("admin", record.operatorUsername());
        assertEquals(AdminAuditResourceType.ARTICLE, record.resourceType());
        assertEquals("40001", record.resourceId());
        assertEquals(AdminAuditAction.CREATE, record.action());
        assertEquals("PUBLISHED", record.actionDetail());
        assertEquals(AdminAuditResult.SUCCESS, record.result());
        assertEquals("POST", record.requestMethod());
        assertEquals("/api/v1/admin/articles", record.requestPath());
    }

    @Test
    void shouldRecordBusinessFailureAndKeepOriginalException() {
        BizException exception = assertThrows(BizException.class, () -> target.fail(40002L));

        assertEquals(ResultCode.ARTICLE_NOT_FOUND, exception.getResultCode());
        ArgumentCaptor<AdminAuditRecordBO> captor = ArgumentCaptor.forClass(AdminAuditRecordBO.class);
        verify(auditLogService).recordFailure(captor.capture());
        AdminAuditRecordBO record = captor.getValue();
        assertEquals("40002", record.resourceId());
        assertEquals(AdminAuditResult.FAILURE, record.result());
        assertEquals(ResultCode.ARTICLE_NOT_FOUND.getCode(), record.failureCode());
        assertEquals(ResultCode.ARTICLE_NOT_FOUND.getMessage(), record.failureMessage());
    }

    @Test
    void auditStorageFailureShouldFailTheAtomicOperationAndRecordFailure() {
        doThrow(new RuntimeException("audit database unavailable"))
                .when(auditLogService).recordSuccess(any(AdminAuditRecordBO.class));

        assertThrows(RuntimeException.class, () -> target.create(40003L));
        verify(auditLogService).recordFailure(any(AdminAuditRecordBO.class));
    }

    static class AuditedTarget {

        @AdminAudit(
                resourceType = AdminAuditResourceType.ARTICLE,
                action = AdminAuditAction.CREATE,
                resourceId = "#result",
                detail = "'PUBLISHED'"
        )
        public Long create(Long articleId) {
            return articleId;
        }

        @AdminAudit(
                resourceType = AdminAuditResourceType.ARTICLE,
                action = AdminAuditAction.DELETE,
                resourceId = "#p0"
        )
        public void fail(Long articleId) {
            throw new BizException(ResultCode.ARTICLE_NOT_FOUND);
        }
    }
}
