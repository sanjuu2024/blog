package com.ccsanjuu.blog.common.ratelimit;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LikeRateLimiter {

    private static final int LIMIT = 30;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String KEY_PREFIX = "blog:like:rate:actor:";
    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT = new DefaultRedisScript<>(
            "local count = tonumber(redis.call('get', KEYS[1]) or '0') "
                    + "if count >= tonumber(ARGV[1]) then return 1 end "
                    + "local nextCount = redis.call('incr', KEYS[1]) "
                    + "if nextCount == 1 then redis.call('pexpire', KEYS[1], ARGV[2]) end "
                    + "return 0",
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 原子预占当前主体每分钟的点赞和取消额度。
     *
     * @param identity 登录用户 ID 或游客匿名 token 哈希
     * @param rateLimitedResultCode 当前接口对应的限流业务码
     */
    public void acquire(String identity, ResultCode rateLimitedResultCode) {
        Long result = stringRedisTemplate.execute(
                ACQUIRE_SCRIPT,
                List.of(KEY_PREFIX + TokenHashUtil.sha256(identity)),
                String.valueOf(LIMIT),
                String.valueOf(WINDOW.toMillis())
        );
        if (result == null || result != 0L) {
            throw new BizException(rateLimitedResultCode);
        }
    }
}
