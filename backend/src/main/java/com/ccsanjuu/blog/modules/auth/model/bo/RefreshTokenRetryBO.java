package com.ccsanjuu.blog.modules.auth.model.bo;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * 成功轮转后供短暂重试使用的令牌结果，仅以密文缓存。
 *
 * @param tokenVersion 签发时的用户令牌版本
 * @param accessToken 原轮转产生的 Access Token
 * @param accessTokenExpiresAt Access Token 到期时间
 * @param refreshToken 原轮转产生的 Refresh Token
 * @param refreshTokenExpiresAt Refresh Token 到期时间
 * @param retryUntil 固定重试截止时间
 * @param previousTokenHash 对应旧令牌摘要，防止缓存值被挪用于其他会话
 */
public record RefreshTokenRetryBO(
        long tokenVersion,
        String accessToken,
        OffsetDateTime accessTokenExpiresAt,
        String refreshToken,
        OffsetDateTime refreshTokenExpiresAt,
        Instant retryUntil,
        String previousTokenHash
) {

    @Override
    public String toString() {
        return "RefreshTokenRetryBO[REDACTED]";
    }
}
