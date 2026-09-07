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
    TAG_HAS_ARTICLES(105003, "该标签下存在文章，请先迁移文章或删除文章", 409),

    // comment 模块 06xxxx
    COMMENT_NOT_FOUND(106001, "评论不存在", 404),
    COMMENT_DISABLED(106002, "文章已关闭评论", 409),
    COMMENT_PARENT_UNAVAILABLE(106003, "回复目标不可用", 409),
    COMMENT_RATE_LIMITED(106004, "评论过于频繁，请稍后再试", 429),
    COMMENT_NO_PERMISSION(106005, "无权操作该评论", 403),
    COMMENT_STATUS_TRANSITION_INVALID(106006, "评论状态流转不合法", 409),
    COMMENT_MODERATION_REASON_REQUIRED(106007, "评论处理原因不能为空", 400),

    // file 模块 07xxxx
    IMAGE_REQUIRED(107001, "请选择需要上传的图片", 400),
    IMAGE_TYPE_NOT_SUPPORTED(107002, "仅支持 JPG、JPEG、PNG、WebP 和 GIF 图片", 415),
    IMAGE_TOO_LARGE(107003, "图片大小超过限制", 413),
    IMAGE_UPLOAD_FAILED(107004, "图片上传失败，请稍后重试", 502),
    AVATAR_UPLOAD_RATE_LIMITED(107005, "头像上传过于频繁，请稍后再试", 429),

    // message 模块 08xxxx
    MESSAGE_NOT_FOUND(108001, "留言不存在", 404),
    MESSAGE_PARENT_UNAVAILABLE(108003, "留言回复目标不可用", 409),
    MESSAGE_RATE_LIMITED(108004, "留言过于频繁，请稍后再试", 429),
    MESSAGE_NO_PERMISSION(108005, "无权操作该留言", 403),
    MESSAGE_STATUS_TRANSITION_INVALID(108006, "留言状态流转不合法", 409),
    MESSAGE_MODERATION_REASON_REQUIRED(108007, "留言处理原因不能为空", 400),
    MESSAGE_NICKNAME_INVALID(108008, "留言昵称不合法", 400),
    MESSAGE_EMAIL_REQUIRED(108009, "勾选回复通知时必须填写邮箱", 400),
    MESSAGE_UNSUBSCRIBE_TOKEN_INVALID(108010, "留言退订链接无效或已失效", 400),
    MESSAGE_BATCH_INVALID(108011, "批量通过的留言必须是待审核顶层留言", 409),
    MESSAGE_BATCH_TOO_LARGE(108012, "一次最多通过 100 条留言", 400),
    MESSAGE_MAIL_CONFIG_INVALID(108013, "留言通知邮件配置不完整", 500);

    private final int code;
    private final String message;
    private final int httpStatus;

    ResultCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
