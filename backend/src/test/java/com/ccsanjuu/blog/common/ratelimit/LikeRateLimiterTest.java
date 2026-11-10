package com.ccsanjuu.blog.common.ratelimit;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.util.TokenHashUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LikeRateLimiterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void acquireShouldStoreOnlyHashedActorIdentity() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L);

        new LikeRateLimiter(stringRedisTemplate).acquire("user:10001", ResultCode.ARTICLE_LIKE_RATE_LIMITED);

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(stringRedisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), any(Object[].class));
        assertEquals(List.of(
                "blog:like:rate:actor:" + TokenHashUtil.sha256("user:10001")
        ), keysCaptor.getValue());
    }

    @Test
    void acquireShouldReturnTheEndpointSpecificRateLimitCode() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        BizException exception = assertThrows(BizException.class,
                () -> new LikeRateLimiter(stringRedisTemplate)
                        .acquire("visitor:sha256:visitor-hash", ResultCode.COMMENT_LIKE_RATE_LIMITED));

        assertEquals(ResultCode.COMMENT_LIKE_RATE_LIMITED, exception.getResultCode());
    }
}
