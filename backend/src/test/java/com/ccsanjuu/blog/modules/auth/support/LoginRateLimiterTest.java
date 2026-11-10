package com.ccsanjuu.blog.modules.auth.support;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.util.TokenHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LoginRateLimiterTest {

    private static final String CLIENT_IP = "203.0.113.10";
    private static final Long USER_ID = 10001L;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginRateLimiter loginRateLimiter;

    @BeforeEach
    void setUp() {
        loginRateLimiter = new LoginRateLimiter(stringRedisTemplate);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void checkIpAllowedShouldRejectAfterTheIpFailureLimit() {
        when(valueOperations.get(ipKey())).thenReturn("30");

        BizException exception = assertThrows(BizException.class,
                () -> loginRateLimiter.checkIpAllowed(CLIENT_IP));

        assertEquals(ResultCode.LOGIN_RATE_LIMITED, exception.getResultCode());
    }

    @Test
    void checkAccountAllowedShouldRejectAfterTheAccountIpFailureLimit() {
        when(valueOperations.get(accountIpKey())).thenReturn("5");

        BizException exception = assertThrows(BizException.class,
                () -> loginRateLimiter.checkAccountAllowed(CLIENT_IP, USER_ID));

        assertEquals(ResultCode.LOGIN_RATE_LIMITED, exception.getResultCode());
    }

    @Test
    void recordFailureShouldUseHashedIpAndAccountKeys() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        loginRateLimiter.recordFailure(CLIENT_IP, USER_ID);

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(stringRedisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), any(Object[].class));
        assertEquals(List.of(ipKey(), accountIpKey()), keysCaptor.getValue());
    }

    @Test
    void successfulLoginShouldClearOnlyTheAccountIpFailureCounter() {
        when(stringRedisTemplate.delete(accountIpKey())).thenReturn(true);

        loginRateLimiter.clearAccountFailures(CLIENT_IP, USER_ID);

        verify(stringRedisTemplate).delete(accountIpKey());
    }

    private String ipKey() {
        return "blog:auth:login:failures:ip:" + TokenHashUtil.sha256(CLIENT_IP);
    }

    private String accountIpKey() {
        return "blog:auth:login:failures:user-ip:" + TokenHashUtil.sha256(USER_ID + ":" + CLIENT_IP);
    }
}
