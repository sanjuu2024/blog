package com.ccsanjuu.blog.modules.audit.model.dto;

import com.ccsanjuu.blog.common.api.PageQuery;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResult;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminAuditLogQueryDTO extends PageQuery {

    @Positive(message = "操作者 ID 必须大于 0")
    private Long operatorId;

    private AdminAuditResourceType resourceType;

    @Size(max = 255, message = "目标资源标识筛选长度不能超过 255 个字符")
    private String resourceId;

    private AdminAuditAction action;

    private AdminAuditResult result;

    private OffsetDateTime createdAtFrom;

    private OffsetDateTime createdAtTo;
}
