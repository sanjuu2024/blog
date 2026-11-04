package com.ccsanjuu.blog.modules.auth.support;

import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.common.util.TokenHashUtil;
import com.ccsanjuu.blog.modules.auth.model.bo.AuthTokenPair;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRetryCacheTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> values;

    private RefreshTokenRetryCache cache;
    private AuthTokenPair tokens;

    @BeforeEach
    void setUp() {
        cache = new RefreshTokenRetryCache(redis, new ObjectMapper().registerModule(new JavaTimeModule()),
                JwtUtil.createHmacShaKey("0123456789abcdef0123456789abcdef"));
        tokens = AuthTokenPair.builder().accessToken("secret-access-token").refreshToken("secret-refresh-token")
                .accessTokenExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15))
                .refreshTokenExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(7)).build();
    }

    @Test
    void shouldEncryptTokensAndReuseFixedResultWithoutExtendingTtl() {
        when(redis.opsForValue()).thenReturn(values);
        Instant deadline = Instant.now().plusSeconds(5);
        cache.store("old-token", 3L, tokens, deadline);
        ArgumentCaptor<String> ciphertext = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(values).set(eq("blog:auth:refresh-retry:" + TokenHashUtil.sha256("old-token")),
                ciphertext.capture(), ttl.capture());
        assertFalse(ciphertext.getValue().contains(tokens.getRefreshToken()));
        assertFalse(ciphertext.getValue().contains(tokens.getAccessToken()));
        assertTrue(ttl.getValue().isPositive());
        assertTrue(ttl.getValue().compareTo(Duration.ofSeconds(5)) <= 0);
        when(values.get(anyString())).thenReturn(ciphertext.getValue());

        var first = cache.find("old-token");
        var second = cache.find("old-token");

        assertNotNull(first);
        assertEquals(tokens.getRefreshToken(), first.refreshToken());
        assertEquals(tokens.getAccessToken(), first.accessToken());
        assertEquals(deadline, first.retryUntil());
        assertEquals(first, second);
        assertFalse(first.toString().contains(tokens.getRefreshToken()));
        verify(values, times(1)).set(anyString(), anyString(), any(Duration.class));
        assertNull(cache.find("different-old-token"));
    }

    @Test
    void shouldNotStoreAnAlreadyExpiredResult() {
        cache.store("old-token", 3L, tokens, Instant.now().minusSeconds(1));

        verify(redis, never()).opsForValue();
    }

    @Test
    void shouldRejectUnreadableCiphertext() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn("not-encrypted-token-data");

        assertNull(cache.find("old-token"));
    }

    @Test
    void redisWriteFailureShouldNotInterruptNormalRotation() {
        when(redis.opsForValue()).thenReturn(values);
        doThrow(new RedisConnectionFailureException("test redis unavailable"))
                .when(values).set(anyString(), anyString(), any(Duration.class));

        assertDoesNotThrow(() -> cache.store("old-token", 3L, tokens, Instant.now().plusSeconds(5)));
    }

    @Test
    void redisReadFailureShouldDisableRetry() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenThrow(new RedisConnectionFailureException("test redis unavailable"));

        assertNull(cache.find("old-token"));
    }
}
