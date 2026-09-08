package com.ccsanjuu.blog.modules.audit.model.enums;

/**
 * 后台审计日志的管理操作类型。
 */
public enum AdminAuditAction {
    CREATE,
    UPDATE,
    DELETE,
    CHANGE_STATUS,
    CHANGE_ROLE,
    MODERATE,
    REPLY,
    BATCH_APPROVE,
    UPLOAD
}
