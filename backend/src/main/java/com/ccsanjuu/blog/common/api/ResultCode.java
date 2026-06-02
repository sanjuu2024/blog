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

    // article 模块 03xxxx
    ARTICLE_NOT_FOUND(103001, "文章不存在", 404),
    ARTICLE_CATEGORY_NOT_FOUND(103002, "文章分类不存在", 404),
    ARTICLE_CATEGORY_LEVEL_INVALID(103003, "文章只能绑定二级分类", 400),
    ARTICLE_CATEGORY_DISABLED(103004, "文章分类已禁用", 409),
    ARTICLE_TAG_NOT_FOUND(103005, "文章标签不存在", 404),
    ARTICLE_TAG_DISABLED(103006, "文章标签已禁用", 409),
    ARTICLE_STATUS_TRANSITION_INVALID(103007, "文章状态流转不合法", 409),
    ARTICLE_NOT_VISIBLE(103008, "文章当前不可见", 404),
    ARTICLE_AUTHOR_NOT_FOUND(103009, "文章作者不存在", 404),

    // category 模块 04xxxx
    CATEGORY_NAME_ALREADY_EXISTS(104001, "分类名称已存在", 409),
    CATEGORY_HAS_CHILDREN(104002, "该分类下存在子分类，请先删除或迁移子分类", 409),
    CATEGORY_HAS_ARTICLES(104003, "该分类下存在文章，请先迁移文章或删除文章", 409),
    CATEGORY_NOT_FOUND(104004, "该分类不存在", 404),
    CATEGORY_PARENT_REQUIRED(104005, "二级分类必须指定父分类", 400),
    CATEGORY_PARENT_NOT_ALLOWED(104006, "一级分类不能指定父分类", 400),
    CATEGORY_PARENT_INVALID(104007, "父分类不存在或父分类不是一级分类", 400),
    CATEGORY_UPDATE_LEVEL_NOT_ALLOWED(104008, "不允许更新分类的级别", 409),

    // tag 模块 05xxxx
    TAG_NAME_ALREADY_EXISTS(105001, "标签名称已存在", 409),
    TAG_NOT_FOUND(105002, "该标签不存在", 404),
    TAG_HAS_ARTICLES(105003, "该标签下存在文章，请先迁移文章或删除文章", 409);

    private final int code;
    private final String message;
    private final int httpStatus;

    ResultCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
