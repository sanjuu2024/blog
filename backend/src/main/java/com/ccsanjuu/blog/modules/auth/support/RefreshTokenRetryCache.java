package com.ccsanjuu.blog.modules.auth.support;

import com.ccsanjuu.blog.common.util.TokenHashUtil;
import com.ccsanjuu.blog.modules.auth.model.bo.AuthTokenPair;
import com.ccsanjuu.blog.modules.auth.model.bo.RefreshTokenRetryBO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * 使用固定截止时间和 AES-GCM 密文保存短暂的轮转结果，Redis 故障时退回严格轮转。
 */
@Slf4j
@Component
public class RefreshTokenRetryCache {

    public static final Duration RETRY_WINDOW = Duration.ofSeconds(5);
    private static final String KEY_PREFIX = "blog:auth:refresh-retry:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TextEncryptor encryptor;

    public RefreshTokenRetryCache(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            SecretKey jwtSigningKey
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        // 独立用途 salt 派生缓存加密密钥，令牌原文不进入 Redis 或日志。
        this.encryptor = Encryptors.delux(HexFormat.of().formatHex(jwtSigningKey.getEncoded()),
                "726566726573682d72657472792d7631");
    }

    /**
     * 在持有用户行锁的轮转事务中保存结果；数据库回滚后缓存不能单独使会话有效。
     *
     * @param previousToken 本次轮转的旧 Refresh Token
     * @param tokenVersion 签发时的用户令牌版本
     * @param tokens 本次成功签发的令牌
     * @param retryUntil 首次轮转的固定重试截止时间
     */
    public void store(String previousToken, long tokenVersion, AuthTokenPair tokens, Instant retryUntil) {
        try {
            Duration remaining = Duration.between(Instant.now(), retryUntil);
            if (remaining.isNegative() || remaining.isZero()) {
                return;
            }
            RefreshTokenRetryBO result = new RefreshTokenRetryBO(tokenVersion,
                    tokens.getAccessToken(), tokens.getAccessTokenExpiresAt(),
                    tokens.getRefreshToken(), tokens.getRefreshTokenExpiresAt(), retryUntil,
                    TokenHashUtil.sha256(previousToken));
            String encrypted = encryptor.encrypt(objectMapper.writeValueAsString(result));
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + TokenHashUtil.sha256(previousToken),
                    encrypted, remaining);
        } catch (Exception exception) {
            log.warn("security_event=REFRESH_RETRY_CACHE_WRITE_FAILED errorType={}",
                    exception.getClass().getSimpleName());
        }
    }

    /**
     * 读取未过期的轮转结果，不延长缓存或结果自身的截止时间。
     *
     * @param previousToken 待重试的旧 Refresh Token
     * @return 有效缓存；缺失、损坏、过期或 Redis 不可用时为空
     */
    public RefreshTokenRetryBO find(String previousToken) {
        try {
            String encrypted = stringRedisTemplate.opsForValue()
                    .get(KEY_PREFIX + TokenHashUtil.sha256(previousToken));
            if (encrypted == null) {
                return null;
            }
            RefreshTokenRetryBO result = objectMapper.readValue(encryptor.decrypt(encrypted),
                    RefreshTokenRetryBO.class);
            return result.previousTokenHash().equals(TokenHashUtil.sha256(previousToken))
                    && result.retryUntil().isAfter(Instant.now()) ? result : null;
        } catch (Exception exception) {
            log.warn("security_event=REFRESH_RETRY_CACHE_READ_FAILED errorType={}",
                    exception.getClass().getSimpleName());
            return null;
        }
    }
}
