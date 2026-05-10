package com.ccsanjuu.blog.common.api;

import lombok.Getter;

@Getter
public enum ResultCode {
    // 通用
    SUCCESS(0, "成功", 200),
    PARAM_INVALID(199001, "请求参数不合法", 400),
    RESOURCE_NOT_FOUND(199404, "请求资源不存在", 404),
    SYSTEM_ERROR(299001, "系统内部异常", 500),

    // auth 模块 01xxxx
    ACCESS_TOKEN_INVALID(101001, "未登录或 Access Token 无效", 401),
    ACCESS_TOKEN_EXPIRED(101002, "Access Token 已过期", 401),
    NO_PERMISSION(101003, "无权限访问", 403),
    REFRESH_TOKEN_INVALID_OR_EXPIRED(101004, "Refresh Token 无效或已过期", 401),

    // user 模块 02xxxx
    USER_NOT_FOUND(102001, "用户不存在", 404),
    USERNAME_EXISTS(102002, "用户名已存在", 409),
    PASSWORD_ERROR(102003, "账号或密码错误", 401),
    OLD_PASSWORD_ERROR(102006, "原密码错误", 400),
    EMAIL_EXISTS(102004, "邮箱已存在", 409),
    USER_DISABLED(102005, "用户已被禁用", 403),
    SELF_ROLE_CHANGE_NOT_ALLOWED(102007, "当前用户不允许修改自己的角色", 403),
    SELF_STATUS_CHANGE_NOT_ALLOWED(102008, "当前用户不允许修改自己的状态", 403),



    // category 模块 04xxxx
    CATEGORY_HAS_ARTICLES(104001, "分类下存在文章，不能删除", 409);

    private final int code;
    private final String message;
    private final int httpStatus;

    ResultCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
