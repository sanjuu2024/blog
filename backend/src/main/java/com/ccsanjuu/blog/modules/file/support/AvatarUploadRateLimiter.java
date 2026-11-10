package com.ccsanjuu.blog.modules.file.support;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AvatarUploadRateLimiter {

    private static final int DAILY_LIMIT = 10;
    private static final Duration MINUTE_DURATION = Duration.ofMinutes(1);
    private static final Duration DAILY_DURATION = Duration.ofHours(24);
    private static final String KEY_PREFIX = "blog:image:avatar:rate:user:";
    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('exists', KEYS[1]) == 1 then return 1 end "
                    + "local count = tonumber(redis.call('get', KEYS[2]) or '0') "
                    + "if count >= tonumber(ARGV[1]) then return 2 end "
                    + "redis.call('set', KEYS[1], ARGV[2], 'PX', ARGV[3]) "
                    + "local newCount = redis.call('incr', KEYS[2]) "
                    + "if newCount == 1 then redis.call('pexpire', KEYS[2], ARGV[4]) end "
                    + "return 0",
            Long.class
    );
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) ~= ARGV[1] then return 0 end "
                    + "redis.call('del', KEYS[1]) "
                    + "local count = tonumber(redis.call('get', KEYS[2]) or '0') "
                    + "if count <= 1 then redis.call('del', KEYS[2]) else redis.call('decr', KEYS[2]) end "
                    + "return 1",
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 原子检查并预占一分钟和 24 小时两级限流额度。
     *
     * @param userId 用户 ID
     */
    public void acquire(Long userId) {
        String minuteKey = KEY_PREFIX + userId + ":minute";
        String dailyKey = KEY_PREFIX + userId + ":day";
        String marker = UUID.randomUUID().toString();

        registerRollbackCallback(userId, minuteKey, dailyKey, marker);
        Long result = stringRedisTemplate.execute(
                ACQUIRE_SCRIPT,
                List.of(minuteKey, dailyKey),
                String.valueOf(DAILY_LIMIT),
                marker,
                String.valueOf(MINUTE_DURATION.toMillis()),
                String.valueOf(DAILY_DURATION.toMillis())
        );
        if (result == null || result != 0L) {
            throw new BizException(ResultCode.AVATAR_UPLOAD_RATE_LIMITED);
        }
    }

    /**
     * 数据库事务回滚或上传失败时释放本次额度；marker 防止误删后续请求的限流 key。
     *
     * @param userId 用户 ID
     * @param minuteKey 一分钟限流 key
     * @param dailyKey 24 小时限流 key
     * @param marker 本次额度预占标识
     */
    private void registerRollbackCallback(Long userId, String minuteKey, String dailyKey, String marker) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                try {
                    stringRedisTemplate.execute(RELEASE_SCRIPT, List.of(minuteKey, dailyKey), marker);
                } catch (RuntimeException exception) {
                    // 清理失败不覆盖原始异常，两级限流 key 都会在 TTL 到期后自动释放。
                    log.warn("头像上传回滚时释放 Redis 限流额度失败: userId={}", userId, exception);
                }
            }
        });
    }
}
