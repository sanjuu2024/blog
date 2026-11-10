package com.ccsanjuu.blog.modules.audit.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("blog_admin_audit_log")
public class AdminAuditLog {

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

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
