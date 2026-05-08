package com.ccsanjuu.blog.common.api;

import lombok.Getter;

@Getter
public enum ResultCode {
    // 通用
    SUCCESS(0,"成功", 200),
    PARAM_INVALID(199001, "参数校验失败", 400),
    SYSTEM_ERROR(299001, "系统内部异常", 500),

    // auth 模块 01xxxx
    TOKEN_INVALID(101001, "未登录或令牌无效", 401),
    TOKEN_EXPIRED(101002, "令牌已过期", 401),
    NO_PERMISSION(101003, "无权限访问", 403),

    // user 模块 02xxxx
    USER_NOT_FOUND(102001, "用户不存在", 404),
    USERNAME_EXISTS(102002, "用户名已存在", 409),
    PASSWORD_ERROR(102003, "账号或密码错误", 401);

    private final int code;
    private final String message;
    private final int httpStatus;

    ResultCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
