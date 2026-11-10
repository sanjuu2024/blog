package com.ccsanjuu.blog.modules.message.support;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class MessageRateLimiterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void acquireShouldRejectWhenRedisReportsLimitExceeded() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        BizException exception = assertThrows(BizException.class,
                () -> new MessageRateLimiter(stringRedisTemplate).acquire("ip:127.0.0.1"));

        assertEquals(ResultCode.MESSAGE_RATE_LIMITED, exception.getResultCode());
    }

    @Test
    void rejectedRequestShouldNotRegisterRollbackCallback() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);
        TransactionSynchronizationManager.initSynchronization();

        assertThrows(BizException.class,
                () -> new MessageRateLimiter(stringRedisTemplate).acquire("ip:127.0.0.1"));

        assertEquals(0, TransactionSynchronizationManager.getSynchronizations().size());
    }

    @Test
    void rollbackShouldReleaseBothWindows() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L);
        TransactionSynchronizationManager.initSynchronization();

        new MessageRateLimiter(stringRedisTemplate).acquire("user:10001");
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(item -> item.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(stringRedisTemplate, times(2)).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }
}
