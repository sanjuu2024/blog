package com.ccsanjuu.blog.modules.auth.service;

import com.ccsanjuu.blog.modules.auth.model.enums.EmailVerificationPurpose;

/**
 * 统一处理不同业务场景的邮箱验证码。
 */
public interface EmailVerificationService {

    /**
     * 向指定用途的邮箱发送验证码。
     *
     * @param email 收件邮箱
     * @param clientIp 客户端 IP
     * @param purpose 验证码用途
     */
    void sendCode(String email, String clientIp, EmailVerificationPurpose purpose);

    /**
     * 校验指定用途的邮箱验证码。
     *
     * @param email 收件邮箱
     * @param code 验证码
     * @param purpose 验证码用途
     */
    void verifyCode(String email, String code, EmailVerificationPurpose purpose);

    /**
     * 清理指定用途的邮箱验证码。
     *
     * @param email 收件邮箱
     * @param purpose 验证码用途
     */
    void clearCode(String email, EmailVerificationPurpose purpose);
}
