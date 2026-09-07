package com.ccsanjuu.blog.modules.message.support;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRateLimiter {

    private static final int HOURLY_LIMIT = 10;
    private static final Duration SHORT_WINDOW = Duration.ofSeconds(30);
    private static final Duration LONG_WINDOW = Duration.ofHours(1);
    private static final String KEY_PREFIX = "blog:message:rate:";
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
            "if redis.call('get', KEYS[1]) == ARGV[1] then redis.call('del', KEYS[1]) end "
                    + "local count = tonumber(redis.call('get', KEYS[2]) or '0') "
                    + "if count <= 1 then redis.call('del', KEYS[2]) else redis.call('decr', KEYS[2]) end "
                    + "return 1",
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 原子预占 30 秒和 1 小时两级留言额度。
     *
     * @param identity 已登录用户 ID 或游客客户端 IP
     */
    public void acquire(String identity) {
        String identityHash = hash(identity);
        String shortKey = KEY_PREFIX + identityHash + ":short";
        String hourlyKey = KEY_PREFIX + identityHash + ":hour";
        String marker = UUID.randomUUID().toString();

        Long result = stringRedisTemplate.execute(
                ACQUIRE_SCRIPT,
                List.of(shortKey, hourlyKey),
                String.valueOf(HOURLY_LIMIT),
                marker,
                String.valueOf(SHORT_WINDOW.toMillis()),
                String.valueOf(LONG_WINDOW.toMillis())
        );
        if (result == null || result != 0L) {
            throw new BizException(ResultCode.MESSAGE_RATE_LIMITED);
        }
        registerRollbackCallback(shortKey, hourlyKey, marker);
    }

    /**
     * 使用 SHA-256 避免把原始 IP 或用户标识直接写入 Redis key。
     *
     * @param identity 限流身份
     * @return 十六进制摘要
     */
    private String hash(String identity) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    /**
     * 事务回滚时释放预占额度，避免失败请求消耗用户配额。
     * 短窗口过期或已属于后续请求时只归还小时计数，不删除新的短窗口。
     *
     * @param shortKey 短窗口 key
     * @param hourlyKey 长窗口 key
     * @param marker 本次请求的唯一标记
     */
    private void registerRollbackCallback(String shortKey, String hourlyKey, String marker) {
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
                    stringRedisTemplate.execute(RELEASE_SCRIPT, List.of(shortKey, hourlyKey), marker);
                } catch (RuntimeException exception) {
                    log.warn("留言事务回滚时释放 Redis 限流额度失败", exception);
                }
            }
        });
    }
}
