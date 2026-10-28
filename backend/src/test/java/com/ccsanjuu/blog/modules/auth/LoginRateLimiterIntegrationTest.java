package com.ccsanjuu.blog.modules.auth;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.util.TokenHashUtil;
import com.ccsanjuu.blog.modules.auth.support.LoginRateLimiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
class LoginRateLimiterIntegrationTest {

    @Autowired
    private LoginRateLimiter loginRateLimiter;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private String ipKey;
    private String accountIpKey;

    @AfterEach
    void clearRateLimitKeys() {
        if (ipKey != null) {
            stringRedisTemplate.delete(ipKey);
        }
        if (accountIpKey != null) {
            stringRedisTemplate.delete(accountIpKey);
        }
    }

    @Test
    void accountIpFailuresShouldBlockOnlyThatAccountAndIpPair() {
        String clientIp = "login-test:" + UUID.randomUUID();
        Long userId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        ipKey = "blog:auth:login:failures:ip:" + TokenHashUtil.sha256(clientIp);
        accountIpKey = "blog:auth:login:failures:user-ip:"
                + TokenHashUtil.sha256(userId + ":" + clientIp);
        stringRedisTemplate.delete(ipKey);
        stringRedisTemplate.delete(accountIpKey);

        for (int attempt = 0; attempt < 5; attempt++) {
            loginRateLimiter.recordFailure(clientIp, userId);
        }

        assertDoesNotThrow(() -> loginRateLimiter.checkIpAllowed(clientIp));
        BizException blockedAccount = assertThrows(BizException.class,
                () -> loginRateLimiter.checkAccountAllowed(clientIp, userId));
        assertEquals(ResultCode.LOGIN_RATE_LIMITED, blockedAccount.getResultCode());

        loginRateLimiter.clearAccountFailures(clientIp, userId);

        assertDoesNotThrow(() -> loginRateLimiter.checkAccountAllowed(clientIp, userId));
        assertDoesNotThrow(() -> loginRateLimiter.checkIpAllowed(clientIp));
    }

    @Test
    void ipFailuresShouldBlockUnknownAccountsAtTheIpThreshold() {
        String clientIp = "login-test:" + UUID.randomUUID();
        ipKey = "blog:auth:login:failures:ip:" + TokenHashUtil.sha256(clientIp);
        stringRedisTemplate.delete(ipKey);

        for (int attempt = 0; attempt < 30; attempt++) {
            loginRateLimiter.recordFailure(clientIp, null);
        }

        BizException blockedIp = assertThrows(BizException.class,
                () -> loginRateLimiter.checkIpAllowed(clientIp));
        assertEquals(ResultCode.LOGIN_RATE_LIMITED, blockedIp.getResultCode());
    }
}
