package com.ccsanjuu.blog.modules.auth.support;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private static final int ACCOUNT_IP_FAILURE_LIMIT = 5;
    private static final int IP_FAILURE_LIMIT = 30;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final String KEY_PREFIX = "blog:auth:login:failures:";
    private static final DefaultRedisScript<Long> RECORD_FAILURE_SCRIPT = new DefaultRedisScript<>(
            "local ipCount = redis.call('incr', KEYS[1]) "
                    + "if ipCount == 1 then redis.call('pexpire', KEYS[1], ARGV[1]) end "
                    + "if ARGV[2] == '1' then "
                    + "local accountIpCount = redis.call('incr', KEYS[2]) "
                    + "if accountIpCount == 1 then redis.call('pexpire', KEYS[2], ARGV[1]) end "
                    + "end "
                    + "return ipCount",
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 检查来源 IP 的登录失败次数。
     *
     * @param clientIp 客户端 IP，由受信任的反向代理提供
     */
    public void checkIpAllowed(String clientIp) {
        if (getCount(ipKey(clientIp)) >= IP_FAILURE_LIMIT) {
            throw new BizException(ResultCode.LOGIN_RATE_LIMITED);
        }
    }

    /**
     * 检查已知账号与来源 IP 组合的登录失败次数。
     *
     * @param clientIp 客户端 IP
     * @param userId 用户 ID；未知账号时不创建账号维度计数
     */
    public void checkAccountAllowed(String clientIp, Long userId) {
        if (userId != null && getCount(accountIpKey(clientIp, userId)) >= ACCOUNT_IP_FAILURE_LIMIT) {
            throw new BizException(ResultCode.LOGIN_RATE_LIMITED);
        }
    }

    /**
     * 记录一次失败登录，并为首次失败的计数键设置 15 分钟过期时间。
     *
     * @param clientIp 客户端 IP
     * @param userId 用户 ID；未知账号时为空
     */
    public void recordFailure(String clientIp, Long userId) {
        String ipKey = ipKey(clientIp);
        String accountKey = userId == null ? ipKey : accountIpKey(clientIp, userId);
        Long result = stringRedisTemplate.execute(
                RECORD_FAILURE_SCRIPT,
                List.of(ipKey, accountKey),
                String.valueOf(FAILURE_WINDOW.toMillis()),
                userId == null ? "0" : "1"
        );
        if (result == null) {
            throw new IllegalStateException("Redis login failure counter did not return a result");
        }
    }

    /**
     * 清除成功登录用户与当前 IP 组合的失败计数。
     *
     * @param clientIp 客户端 IP
     * @param userId 用户 ID
     */
    public void clearAccountFailures(String clientIp, Long userId) {
        stringRedisTemplate.delete(accountIpKey(clientIp, userId));
    }

    private int getCount(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        return StringUtils.hasText(value) ? Integer.parseInt(value) : 0;
    }

    private String ipKey(String clientIp) {
        String normalizedIp = StringUtils.hasText(clientIp) ? clientIp : "unknown";
        return KEY_PREFIX + "ip:" + TokenHashUtil.sha256(normalizedIp);
    }

    private String accountIpKey(String clientIp, Long userId) {
        String normalizedIp = StringUtils.hasText(clientIp) ? clientIp : "unknown";
        return KEY_PREFIX + "user-ip:" + TokenHashUtil.sha256(userId + ":" + normalizedIp);
    }
}
