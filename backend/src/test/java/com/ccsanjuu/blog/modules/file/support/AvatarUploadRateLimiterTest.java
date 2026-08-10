package com.ccsanjuu.blog.modules.file.support;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AvatarUploadRateLimiterTest {

    private static final Long USER_ID = 10001L;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void acquireShouldUseMinuteAndDailyKeys() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L);
        AvatarUploadRateLimiter rateLimiter = new AvatarUploadRateLimiter(stringRedisTemplate);

        rateLimiter.acquire(USER_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(stringRedisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), any(Object[].class));
        assertEquals(List.of(
                "blog:image:avatar:rate:user:10001:minute",
                "blog:image:avatar:rate:user:10001:day"
        ), keysCaptor.getValue());
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L})
    void acquireShouldRejectMinuteOrDailyLimit(long scriptResult) {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(scriptResult);
        AvatarUploadRateLimiter rateLimiter = new AvatarUploadRateLimiter(stringRedisTemplate);

        BizException exception = assertThrows(BizException.class, () -> rateLimiter.acquire(USER_ID));

        assertEquals(ResultCode.AVATAR_UPLOAD_RATE_LIMITED, exception.getResultCode());
    }

    @Test
    void transactionRollbackShouldReleaseAcquiredQuota() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L);
        AvatarUploadRateLimiter rateLimiter = new AvatarUploadRateLimiter(stringRedisTemplate);
        TransactionSynchronizationManager.initSynchronization();

        rateLimiter.acquire(USER_ID);
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(stringRedisTemplate, times(2))
                .execute(any(RedisScript.class), anyList(), any(Object[].class));
    }
}
