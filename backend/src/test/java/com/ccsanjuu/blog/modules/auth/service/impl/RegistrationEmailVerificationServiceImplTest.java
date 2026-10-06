package com.ccsanjuu.blog.modules.auth.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.properties.BlogProperties;
import com.ccsanjuu.blog.properties.BlogMailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import javax.crypto.SecretKey;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class RegistrationEmailVerificationServiceImplTest {

    private static final SecretKey SIGNING_KEY =
            JwtUtil.createHmacShaKey("0123456789abcdef0123456789abcdef");

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    private RegistrationEmailVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        BlogMailProperties mailProperties = new BlogMailProperties();
        mailProperties.setEnabled(true);
        mailProperties.setFrom("noreply@example.com");
        service = new RegistrationEmailVerificationServiceImpl(
                stringRedisTemplate,
                userMapper,
                mailSenderProvider,
                mailProperties,
                new BlogProperties(),
                SIGNING_KEY
        );
    }

    @Test
    void sendCodeShouldStoreHashAndSendSixDigitCode() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L, 1L);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        service.sendCode("Alice@Example.com", "127.0.0.1");

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        String mailText = mailCaptor.getValue().getText();
        assertTrue(mailText != null && mailText.matches("(?s).*\\d{6}.*"));
        assertEquals("alice@example.com", mailCaptor.getValue().getTo()[0]);
    }

    @Test
    void sendCodeShouldRejectRegisteredEmailWithoutConsumingQuota() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        BizException exception = assertThrows(BizException.class,
                () -> service.sendCode("alice@example.com", "127.0.0.1"));

        assertEquals(ResultCode.EMAIL_EXISTS, exception.getResultCode());
        verify(stringRedisTemplate, never()).execute(any(RedisScript.class), anyList(), any(Object[].class));
        verify(mailSenderProvider, never()).getIfAvailable();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendCodeShouldReleaseQuotaWhenSmtpFails() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L, 1L, 1L);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new MailSendException("smtp unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        BizException exception = assertThrows(BizException.class,
                () -> service.sendCode("alice@example.com", "127.0.0.1"));

        assertEquals(ResultCode.EMAIL_VERIFICATION_SEND_FAILED, exception.getResultCode());
        verify(stringRedisTemplate).delete(any(List.class));
    }

    @Test
    void sendCodeShouldNotSendMailWhenCodeCannotBeStored() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L, null, 1L);
        when(userMapper.selectCount(any())).thenReturn(0L);

        BizException exception = assertThrows(BizException.class,
                () -> service.sendCode("alice@example.com", "127.0.0.1"));

        assertEquals(ResultCode.EMAIL_VERIFICATION_SEND_FAILED, exception.getResultCode());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendCodeShouldRejectRateLimitedRequest() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        BizException exception = assertThrows(BizException.class,
                () -> service.sendCode("alice@example.com", "127.0.0.1"));

        assertEquals(ResultCode.EMAIL_VERIFICATION_RATE_LIMITED, exception.getResultCode());
        verify(userMapper).selectCount(any());
    }

    @Test
    void verifyCodeShouldReportAttemptLimit() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(5L);

        BizException exception = assertThrows(BizException.class,
                () -> service.verifyCode("alice@example.com", "000000"));

        assertEquals(ResultCode.EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED, exception.getResultCode());
    }

    @Test
    void verifyCodeShouldTreatFailureCountBelowLimitAsInvalidCode() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(3L);

        BizException exception = assertThrows(BizException.class,
                () -> service.verifyCode("alice@example.com", "000000"));

        assertEquals(ResultCode.EMAIL_VERIFICATION_CODE_INVALID, exception.getResultCode());
    }

    @Test
    void verifyCodeShouldAcceptNegativeOneAsSuccessfulVerification() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(-1L);

        assertDoesNotThrow(() -> service.verifyCode("alice@example.com", "123456"));
    }
}
