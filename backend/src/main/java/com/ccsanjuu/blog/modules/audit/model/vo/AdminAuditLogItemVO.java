package com.ccsanjuu.blog.modules.audit.model.vo;

import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLogItemVO {

    private Long id;

    private Long operatorId;

    private String operatorUsername;

    private AdminAuditResourceType resourceType;

    private String resourceId;

    private AdminAuditAction action;

    private String actionDetail;

    private AdminAuditResult result;

    private Integer failureCode;

    private String failureMessage;

    private String requestMethod;

    private String requestPath;

    private OffsetDateTime createdAt;
}
