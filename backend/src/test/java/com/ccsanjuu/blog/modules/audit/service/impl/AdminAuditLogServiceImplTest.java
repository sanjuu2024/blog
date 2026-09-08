package com.ccsanjuu.blog.modules.audit.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.audit.mapper.AdminAuditLogMapper;
import com.ccsanjuu.blog.modules.audit.model.bo.AdminAuditRecordBO;
import com.ccsanjuu.blog.modules.audit.model.dto.AdminAuditLogQueryDTO;
import com.ccsanjuu.blog.modules.audit.model.entity.AdminAuditLog;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResult;
import com.ccsanjuu.blog.modules.audit.model.vo.AdminAuditLogItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceImplTest {

    @Mock
    private AdminAuditLogMapper adminAuditLogMapper;

    @InjectMocks
    private AdminAuditLogServiceImpl adminAuditLogService;

    @Test
    void shouldPersistAllAuditFields() {
        AdminAuditRecordBO record = new AdminAuditRecordBO(
                10001L,
                "admin",
                AdminAuditResourceType.COMMENT,
                "60001",
                AdminAuditAction.MODERATE,
                "APPROVE",
                AdminAuditResult.SUCCESS,
                null,
                null,
                "PATCH",
                "/api/v1/admin/comments/60001/moderation"
        );

        adminAuditLogService.recordSuccess(record);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogMapper).insert(captor.capture());
        AdminAuditLog saved = captor.getValue();
        assertEquals(record.operatorId(), saved.getOperatorId());
        assertEquals(record.resourceType(), saved.getResourceType());
        assertEquals(record.resourceId(), saved.getResourceId());
        assertEquals(record.actionDetail(), saved.getActionDetail());
        assertEquals(record.result(), saved.getResult());
        assertNull(saved.getFailureCode());
    }

    @Test
    void shouldReturnPagedAuditLogItems() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-09-07T10:00:00Z");
        when(adminAuditLogMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<AdminAuditLog> page = invocation.getArgument(0);
            page.setTotal(1);
            page.setRecords(List.of(AdminAuditLog.builder()
                    .id(1L)
                    .operatorId(10001L)
                    .operatorUsername("admin")
                    .resourceType(AdminAuditResourceType.USER)
                    .resourceId("10002")
                    .action(AdminAuditAction.CHANGE_STATUS)
                    .actionDetail("DISABLED")
                    .result(AdminAuditResult.SUCCESS)
                    .requestMethod("PATCH")
                    .requestPath("/api/v1/admin/users/10002/status")
                    .createdAt(createdAt)
                    .build()));
            return page;
        });
        AdminAuditLogQueryDTO queryDTO = new AdminAuditLogQueryDTO();
        queryDTO.setOperatorId(10001L);
        queryDTO.setResourceType(AdminAuditResourceType.USER);

        PageResult<AdminAuditLogItemVO> result = adminAuditLogService.getAuditLogList(queryDTO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("10002", result.getRecords().getFirst().getResourceId());
        assertEquals(createdAt, result.getRecords().getFirst().getCreatedAt());
    }
}
