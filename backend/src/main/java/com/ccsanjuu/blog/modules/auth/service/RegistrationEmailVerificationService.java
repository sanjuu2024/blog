package com.ccsanjuu.blog.modules.auth.service;

public interface RegistrationEmailVerificationService extends EmailVerificationService {

    /**
     * 向注册邮箱发送验证码。
     *
     * @param email 注册邮箱
     * @param clientIp 客户端 IP
     */
    void sendCode(String email, String clientIp);

    /**
     * 校验注册邮箱验证码，失败时累计错误次数。
     *
     * @param email 注册邮箱
     * @param code 验证码
     */
    void verifyCode(String email, String code);

    /**
     * 注册成功后清理验证码和失败次数。
     *
     * @param email 注册邮箱
     */
    void clearCode(String email);

}
