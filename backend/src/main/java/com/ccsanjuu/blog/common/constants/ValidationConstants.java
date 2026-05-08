package com.ccsanjuu.blog.common.constants;

public final class ValidationConstants {
    private static final String USERNAME_BODY_PATTERN = "[\\p{IsHan}A-Za-z0-9_-]+";
    private static final String ACCOUNT_USERNAME_BODY_PATTERN = "[\\p{IsHan}A-Za-z0-9_-]{4,20}";
    private static final String EMAIL_BODY_PATTERN =
            "[A-Za-z0-9](?:[A-Za-z0-9._%+\\-]{0,62}[A-Za-z0-9])?"
                    + "@(?:[A-Za-z0-9](?:[A-Za-z0-9\\-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}";

    public static final String USERNAME_PATTERN = "^" + USERNAME_BODY_PATTERN + "$";
    public static final String USERNAME_MESSAGE = "用户名只能包含中文、英文、数字、下划线或短横线";

    public static final String EMAIL_PATTERN = "^" + EMAIL_BODY_PATTERN + "$";
    public static final String EMAIL_MESSAGE = "邮箱格式不正确，仅支持常见邮箱格式";

    public static final String ACCOUNT_PATTERN =
            "^(?:" + ACCOUNT_USERNAME_BODY_PATTERN + "|" + EMAIL_BODY_PATTERN + ")$";
    public static final String ACCOUNT_MESSAGE = "登录账号必须是合法用户名或邮箱";

    public static final String PASSWORD_PATTERN =
            "^[\\p{IsHan}A-Za-z0-9_\\-!@#$%^&*()+=\\[\\]{}:;'\".,?/~`|\\\\<>]+$";
    public static final String PASSWORD_MESSAGE =
            "密码只能包含中文、英文、数字、下划线、短横线或常用特殊字符，且不能包含空格";

    private ValidationConstants() {
    }
}
