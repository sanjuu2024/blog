package com.ccsanjuu.blog.modules.auth.service.impl;

import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.properties.JwtProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenVersionServiceImplTest {

    private static final Long USER_ID = 10001L;
    private static final String KEY = "blog:auth:token-version:user:" + USER_ID;
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenVersionServiceImpl tokenVersionService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        JwtProperties jwtProperties = new JwtProperties(
                "sanjuu-blog",
                "unused-in-this-test",
                CACHE_TTL,
                Duration.ofDays(7)
        );
        tokenVersionService = new TokenVersionServiceImpl(userMapper, stringRedisTemplate, jwtProperties);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void getCurrentVersionShouldUseRedisCache() {
        when(valueOperations.get(KEY)).thenReturn("3");

        Long version = tokenVersionService.getCurrentVersion(USER_ID);

        assertEquals(3L, version);
        verifyNoInteractions(userMapper);
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void getCurrentVersionShouldParseTransactionalCacheValue() {
        when(valueOperations.get(KEY)).thenReturn("3:operation-id");

        Long version = tokenVersionService.getCurrentVersion(USER_ID);

        assertEquals(3L, version);
        verifyNoInteractions(userMapper);
    }

    @Test
    void getCurrentVersionShouldLoadDatabaseAndPopulateCache() {
        when(valueOperations.get(KEY)).thenReturn(null);
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .tokenVersion(4L)
                .build());

        Long version = tokenVersionService.getCurrentVersion(USER_ID);

        assertEquals(4L, version);
        verify(valueOperations).set(KEY, "4", CACHE_TTL);
    }

    @Test
    void getCurrentVersionShouldFallbackToDatabaseWhenRedisFails() {
        when(valueOperations.get(KEY)).thenThrow(new RedisConnectionFailureException("Redis unavailable"));
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .tokenVersion(2L)
                .build());

        Long version = tokenVersionService.getCurrentVersion(USER_ID);

        assertEquals(2L, version);
        verify(valueOperations).set(KEY, "2", CACHE_TTL);
    }

    @Test
    void getCurrentVersionShouldReturnNullWhenUserDoesNotExist() {
        when(valueOperations.get(KEY)).thenReturn(null);
        when(userMapper.selectById(USER_ID)).thenReturn(null);

        Long version = tokenVersionService.getCurrentVersion(USER_ID);

        assertNull(version);
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void incrementVersionShouldUpdateDatabaseAndRedis() {
        when(userMapper.incrementTokenVersion(USER_ID)).thenReturn(5L);

        Long version = tokenVersionService.incrementVersion(USER_ID);

        assertEquals(5L, version);
        ArgumentCaptor<String> cacheValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(KEY), cacheValueCaptor.capture(), eq(CACHE_TTL));
        assertTrue(cacheValueCaptor.getValue().startsWith("5:"));
    }

    @Test
    void incrementVersionShouldRestoreCacheWhenTransactionRollsBack() {
        when(userMapper.incrementTokenVersion(USER_ID)).thenReturn(5L);
        TransactionSynchronizationManager.initSynchronization();

        tokenVersionService.incrementVersion(USER_ID);
        ArgumentCaptor<String> cacheValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(KEY), cacheValueCaptor.capture(), eq(CACHE_TTL));
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        synchronizations.getFirst().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        assertEquals(1, synchronizations.size());
        verify(stringRedisTemplate).execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(KEY)),
                eq(cacheValueCaptor.getValue()),
                eq("4"),
                eq(String.valueOf(CACHE_TTL.toMillis()))
        );
    }

    @Test
    void incrementVersionShouldRegisterRollbackBeforeWritingCache() {
        when(userMapper.incrementTokenVersion(USER_ID)).thenReturn(5L);
        TransactionSynchronizationManager.initSynchronization();
        AtomicReference<String> cacheValue = new AtomicReference<>();
        doAnswer(invocation -> {
            cacheValue.set(invocation.getArgument(1));
            assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
            throw new RedisConnectionFailureException("Redis response lost");
        }).when(valueOperations).set(eq(KEY), any(String.class), eq(CACHE_TTL));

        assertThrows(
                RedisConnectionFailureException.class,
                () -> tokenVersionService.incrementVersion(USER_ID)
        );
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        synchronizations.getFirst().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(stringRedisTemplate).execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(KEY)),
                eq(cacheValue.get()),
                eq("4"),
                eq(String.valueOf(CACHE_TTL.toMillis()))
        );
    }
}
