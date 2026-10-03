package com.ccsanjuu.blog.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.auth.service.RegistrationEmailVerificationService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.properties.BlogMailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 处理注册邮箱验证码的发送、限流、校验和清理。
 *
 * <p>验证码及邮箱、IP 标识均只以 HMAC 摘要写入 Redis，完整邮箱仅用于本次 SMTP 投递。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationEmailVerificationServiceImpl implements RegistrationEmailVerificationService {

    private static final int EMAIL_HOURLY_LIMIT = 5;
    private static final int IP_HOURLY_LIMIT = 20;
    private static final int MAX_FAILURES = 5;
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration HOURLY_WINDOW = Duration.ofHours(1);
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "blog:auth:email-verification:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // 原子检查并预占邮箱冷却、邮箱小时额度和 IP 小时额度。
    // KEYS 依次为冷却、邮箱小时计数、IP 小时计数；返回值 0-3 分别表示成功及三种限流原因。
    private static final DefaultRedisScript<Long> ACQUIRE_SEND_QUOTA_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('exists', KEYS[1]) == 1 then return 1 end "
                    + "local emailCount = tonumber(redis.call('get', KEYS[2]) or '0') "
                    + "if emailCount >= tonumber(ARGV[1]) then return 2 end "
                    + "local ipCount = tonumber(redis.call('get', KEYS[3]) or '0') "
                    + "if ipCount >= tonumber(ARGV[2]) then return 3 end "
                    + "redis.call('set', KEYS[1], ARGV[3], 'PX', ARGV[4]) "
                    + "local newEmailCount = redis.call('incr', KEYS[2]) "
                    + "if newEmailCount == 1 then redis.call('pexpire', KEYS[2], ARGV[5]) end "
                    + "local newIpCount = redis.call('incr', KEYS[3]) "
                    + "if newIpCount == 1 then redis.call('pexpire', KEYS[3], ARGV[5]) end "
                    + "return 0",
            Long.class
    );
    // SMTP 发送失败时按本次 marker 释放冷却 key，并归还邮箱与 IP 的小时额度。
    // marker 可避免旧请求误删后续请求已经创建的新冷却窗口。
    private static final DefaultRedisScript<Long> RELEASE_SEND_QUOTA_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then redis.call('del', KEYS[1]) end "
                    + "local emailCount = tonumber(redis.call('get', KEYS[2]) or '0') "
                    + "if emailCount <= 1 then redis.call('del', KEYS[2]) else redis.call('decr', KEYS[2]) end "
                    + "local ipCount = tonumber(redis.call('get', KEYS[3]) or '0') "
                    + "if ipCount <= 1 then redis.call('del', KEYS[3]) else redis.call('decr', KEYS[3]) end "
                    + "return 1",
            Long.class
    );
    // 新验证码覆盖旧验证码时同步清空旧失败次数，并让两项状态共享本次验证码的生命周期。
    private static final DefaultRedisScript<Long> STORE_CODE_SCRIPT = new DefaultRedisScript<>(
            "redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2]) "
                    + "redis.call('del', KEYS[2]) "
                    + "return 1",
            Long.class
    );
    // 原子完成摘要比较和失败计数；第五次失败立即删除验证码，但保留失败标记至原 TTL 到期。
    // 返回 -1 表示成功、0 表示验证码不存在或已过期、正数表示当前累计失败次数。
    private static final DefaultRedisScript<Long> VERIFY_CODE_SCRIPT = new DefaultRedisScript<>(
            "local failures = tonumber(redis.call('get', KEYS[2]) or '0') "
                    + "if failures >= tonumber(ARGV[2]) then return failures end "
                    + "local stored = redis.call('get', KEYS[1]) "
                    + "if not stored then return 0 end "
                    + "if stored == ARGV[1] then return -1 end "
                    + "local newFailures = redis.call('incr', KEYS[2]) "
                    + "if newFailures == 1 then "
                    + "local ttl = redis.call('pttl', KEYS[1]) "
                    + "if ttl > 0 then redis.call('pexpire', KEYS[2], ttl) end end "
                    + "if newFailures >= tonumber(ARGV[2]) then "
                    + "redis.call('del', KEYS[1]) end "
                    + "return newFailures",
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final BlogMailProperties mailProperties;
    private final SecretKey jwtSigningKey;

    /**
     * 申请发送注册邮箱验证码，并在 SMTP 发送失败时归还本次限流额度。
     *
     * @param email 注册邮箱
     * @param clientIp 客户端 IP
     */
    @Override
    public void sendCode(String email, String clientIp) {
        String normalizedEmail = normalizeEmail(email);
        if (emailExists(normalizedEmail)) {
            log.info("security_event=EMAIL_VERIFICATION_REQUEST_REJECTED outcome=FAIL reason=EMAIL_EXISTS recipient={}",
                    maskEmail(normalizedEmail));
            throw new BizException(ResultCode.EMAIL_EXISTS);
        }
        SendQuotaReservation reservation = acquireSendQuota(normalizedEmail, clientIp);

        String code = "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
        String emailHash = hashIdentity(normalizedEmail);
        try {
            // 先保存摘要再发送，避免用户收到一封后端无法校验的验证码邮件。
            Long stored = stringRedisTemplate.execute(
                    STORE_CODE_SCRIPT,
                    List.of(codeKey(emailHash), failureKey(emailHash)),
                    hashVerificationCode(normalizedEmail, code),
                    String.valueOf(CODE_TTL.toMillis())
            );
            if (stored == null) {
                throw new IllegalStateException("邮箱验证码保存失败");
            }
            sendMail(normalizedEmail, code);
            log.info("security_event=EMAIL_VERIFICATION_SENT outcome=SUCCESS recipient={}",
                    maskEmail(normalizedEmail));
        } catch (RuntimeException exception) {
            // 两项补偿均为尽力执行，清理失败不能覆盖对外统一的邮件发送失败结果。
            try {
                stringRedisTemplate.delete(List.of(codeKey(emailHash), failureKey(emailHash)));
            } catch (RuntimeException cleanupException) {
                log.warn("清理发送失败的邮箱验证码时 Redis 操作失败", cleanupException);
            }
            try {
                releaseSendQuota(reservation);
            } catch (RuntimeException cleanupException) {
                log.warn("归还邮箱验证码发送额度时 Redis 操作失败", cleanupException);
            }
            log.warn("security_event=EMAIL_VERIFICATION_SEND_FAILED outcome=FAIL recipient={} reason={}",
                    maskEmail(normalizedEmail), exception.getClass().getSimpleName());
            throw new BizException(ResultCode.EMAIL_VERIFICATION_SEND_FAILED);
        }
    }

    /**
     * 校验注册邮箱验证码，第五次失败时使当前验证码立即失效。
     *
     * @param email 注册邮箱
     * @param code 验证码
     */
    @Override
    public void verifyCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        String emailHash = hashIdentity(normalizedEmail);
        Long result = stringRedisTemplate.execute(
                VERIFY_CODE_SCRIPT,
                List.of(codeKey(emailHash), failureKey(emailHash)),
                hashVerificationCode(normalizedEmail, code),
                String.valueOf(MAX_FAILURES)
        );
        if (result != null && result == -1L) {
            return;
        }
        if (result != null && result >= MAX_FAILURES) {
            throw new BizException(ResultCode.EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED);
        }
        throw new BizException(ResultCode.EMAIL_VERIFICATION_CODE_INVALID);
    }

    /**
     * 注册成功后清理验证码和失败次数。
     *
     * @param email 注册邮箱
     */
    @Override
    public void clearCode(String email) {
        String emailHash = hashIdentity(normalizeEmail(email));
        stringRedisTemplate.delete(List.of(codeKey(emailHash), failureKey(emailHash)));
    }

    /**
     * 通过已配置的同步邮件发送器投递验证码。
     *
     * @param email 完整收件邮箱
     * @param code 6 位数字验证码
     */
    private void sendMail(String email, String code) {
        JavaMailSender mailSender = mailProperties.isEnabled() ? mailSenderProvider.getIfAvailable() : null;
        if (mailSender == null || !StringUtils.hasText(mailProperties.getFrom())) {
            throw new IllegalStateException("邮件发送器不可用");
        }
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(mailProperties.getFrom());
        mail.setTo(email);
        mail.setSubject("Sanjuu Blog 注册邮箱验证码");
        mail.setText("您的注册验证码是：" + code + "\n\n验证码 10 分钟内有效，请勿泄露给他人。");
        mailSender.send(mail);
    }

    /**
     * 原子预占邮箱和客户端 IP 的发送额度。
     *
     * @param email 规范化后的邮箱
     * @param clientIp 客户端 IP
     * @return 本次额度预占信息，用于发送失败时补偿
     */
    private SendQuotaReservation acquireSendQuota(String email, String clientIp) {
        String emailHash = hashIdentity(email);
        String ipHash = hashIdentity(StringUtils.hasText(clientIp) ? clientIp : "unknown");
        String marker = UUID.randomUUID().toString();
        SendQuotaReservation reservation = new SendQuotaReservation(
                KEY_PREFIX + "email:" + emailHash + ":cooldown",
                KEY_PREFIX + "email:" + emailHash + ":hour",
                KEY_PREFIX + "ip:" + ipHash + ":hour",
                marker
        );
        Long result = stringRedisTemplate.execute(
                ACQUIRE_SEND_QUOTA_SCRIPT,
                List.of(reservation.cooldownKey(), reservation.emailHourlyKey(), reservation.ipHourlyKey()),
                String.valueOf(EMAIL_HOURLY_LIMIT),
                String.valueOf(IP_HOURLY_LIMIT),
                marker,
                String.valueOf(SEND_COOLDOWN.toMillis()),
                String.valueOf(HOURLY_WINDOW.toMillis())
        );
        if (result == null || result != 0L) {
            throw new BizException(ResultCode.EMAIL_VERIFICATION_RATE_LIMITED);
        }
        return reservation;
    }

    /**
     * 归还一次未实际完成邮件投递的发送额度。
     *
     * @param reservation 本次额度预占信息
     */
    private void releaseSendQuota(SendQuotaReservation reservation) {
        stringRedisTemplate.execute(
                RELEASE_SEND_QUOTA_SCRIPT,
                List.of(reservation.cooldownKey(), reservation.emailHourlyKey(), reservation.ipHourlyKey()),
                reservation.marker()
        );
    }

    private boolean emailExists(String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .apply("LOWER(email) = {0}", email)) > 0;
    }

    private String hashVerificationCode(String email, String code) {
        return hmac("registration-email-code:" + email + ":" + code);
    }

    /**
     * 对邮箱或 IP 生成带服务端密钥的摘要，避免 Redis key 暴露原始身份。
     *
     * @param identity 邮箱或客户端 IP
     * @return 十六进制 HMAC 摘要
     */
    private String hashIdentity(String identity) {
        return hmac("registration-email-identity:" + identity);
    }

    private String hmac(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(jwtSigningKey);
            return HexFormat.of().formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("邮箱验证码摘要生成失败", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String local = email.substring(0, atIndex);
        return (local.length() <= 2 ? "*".repeat(local.length()) : local.charAt(0) + "***")
                + email.substring(atIndex);
    }

    private String codeKey(String emailHash) {
        return KEY_PREFIX + "email:" + emailHash + ":code";
    }

    private String failureKey(String emailHash) {
        return KEY_PREFIX + "email:" + emailHash + ":failures";
    }

    private record SendQuotaReservation(
            String cooldownKey,
            String emailHourlyKey,
            String ipHourlyKey,
            String marker
    ) {
    }
}
