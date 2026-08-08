package com.ccsanjuu.blog.modules.auth.service.impl;

import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenVersionServiceImpl implements TokenVersionService {

    private static final String KEY_PREFIX = "blog:auth:token-version:user:";
    private static final DefaultRedisScript<Long> RESTORE_VERSION_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "redis.call('set', KEYS[1], ARGV[2], 'PX', ARGV[3]); return 1 "
                    + "else return 0 end",
            Long.class
    );

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    @Override
    public Long getCurrentVersion(Long userId) {
        String key = buildKey(userId);
        try {
            String cachedVersion = stringRedisTemplate.opsForValue().get(key);
            Long version = parseCachedVersion(cachedVersion);
            if (version != null) {
                return version;
            }
        } catch (RuntimeException exception) {
            log.warn(
                    "security_event=TOKEN_VERSION_CACHE_READ_FAILED description=\"读取令牌版本缓存失败，将回源数据库\" outcome=FALLBACK userId={} errorType={}",
                    userId,
                    exception.getClass().getSimpleName()
            );
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        long version = user.getTokenVersion() == null ? 0L : user.getTokenVersion();
        try {
            stringRedisTemplate.opsForValue().set(key, String.valueOf(version), jwtProperties.accessTtl());
        } catch (RuntimeException exception) {
            log.warn(
                    "security_event=TOKEN_VERSION_CACHE_WRITE_FAILED description=\"写入令牌版本缓存失败\" outcome=FAIL userId={} errorType={}",
                    userId,
                    exception.getClass().getSimpleName()
            );
        }
        return version;
    }

    @Override
    @Transactional
    public Long incrementVersion(Long userId) {
        Long newVersion = userMapper.incrementTokenVersion(userId);
        if (newVersion == null) {
            return null;
        }

        String key = buildKey(userId);
        // 操作标识用于区分“版本号相同但来自不同事务”的缓存写入，避免回滚误覆盖后续事务。
        String newValue = newVersion + ":" + UUID.randomUUID();
        String oldValue = String.valueOf(newVersion - 1);

        // 先注册回滚回调，再写 Redis；即使 SET 已执行但客户端收到异常，也有机会恢复缓存。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_ROLLED_BACK) {
                        return;
                    }
                    try {
                        stringRedisTemplate.execute(
                                RESTORE_VERSION_SCRIPT,
                                List.of(key),
                                newValue,
                                oldValue,
                                String.valueOf(jwtProperties.accessTtl().toMillis())
                        );
                    } catch (RuntimeException exception) {
                        log.warn("用户 tokenVersion 事务回滚时恢复 Redis 缓存失败: userId={}", userId, exception);
                    }
                }
            });
        }
        stringRedisTemplate.opsForValue().set(key, newValue, jwtProperties.accessTtl());
        return newVersion;
    }

    /**
     * 解析缓存中的版本号；事务写入值的格式为“版本号:操作标识”。
     *
     * @param cachedValue Redis 缓存值
     * @return 有效版本号，格式错误时返回 null
     */
    private Long parseCachedVersion(String cachedValue) {
        if (!StringUtils.hasText(cachedValue)) {
            return null;
        }

        int separatorIndex = cachedValue.indexOf(':');
        String versionValue = separatorIndex < 0 ? cachedValue : cachedValue.substring(0, separatorIndex);
        try {
            long version = Long.parseLong(versionValue);
            return version >= 0 ? version : null;
        } catch (NumberFormatException ignored) {
            // 缓存内容格式异常时返回 null，由调用方回源数据库。
            return null;
        }
    }

    /**
     * 构建用户 tokenVersion 的 Redis key。
     *
     * @param userId 用户 ID
     * @return Redis key
     */
    private String buildKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
