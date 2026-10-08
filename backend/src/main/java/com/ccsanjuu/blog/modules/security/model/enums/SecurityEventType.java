package com.ccsanjuu.blog.modules.security.model.enums;

/**
 * 账号与认证安全事件类型。
 */
public enum SecurityEventType {
    REGISTER,
    LOGIN,
    LOGOUT,
    TOKEN_REFRESH,
    EMAIL_VERIFICATION,
    PASSWORD_CHANGE,
    EMAIL_CHANGE,
    USER_DELETE,
    USER_STATUS_CHANGE,
    USER_ROLE_CHANGE
}
