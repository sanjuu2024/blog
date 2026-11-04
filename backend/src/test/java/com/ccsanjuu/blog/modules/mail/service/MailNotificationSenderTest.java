package com.ccsanjuu.blog.modules.mail.service;

import com.ccsanjuu.blog.modules.mail.model.bo.MailMessageContent;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import com.ccsanjuu.blog.modules.mail.model.enums.MailDeliveryStatus;
import com.ccsanjuu.blog.properties.BlogMailProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MailNotificationSenderTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MailDeliveryService mailDeliveryService;

    private MailNotificationSender sender;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        BlogMailProperties properties = new BlogMailProperties();
        properties.setEnabled(true);
        properties.setFrom("noreply@example.com");
        sender = new MailNotificationSender(mailSenderProvider, properties, Runnable::run, mailDeliveryService);
        mimeMessage = new MimeMessage(Session.getInstance(System.getProperties()));
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void shouldSendMultipartAlternativeContent() throws Exception {
        sender.submit(
                MailDelivery.builder().id(1L).build(),
                () -> new MailMessageContent("user@example.com", "回复通知", "纯文本", "<p>HTML</p>")
        );

        verify(mailSender).send(mimeMessage);
        verify(mailDeliveryService).updateRecipient(1L, "user@example.com");
        verify(mailDeliveryService).markSent(1L);
        mimeMessage.saveChanges();
        assertTrue(mimeMessage.getContentType().toLowerCase().contains("multipart/alternative"));
    }

    @Test
    void shouldRecordPermanentMailFailure() {
        when(mailSender.createMimeMessage()).thenThrow(new org.springframework.mail.MailPreparationException("failed"));

        sender.submit(
                MailDelivery.builder().id(1L).build(),
                () -> new MailMessageContent("user@example.com", "回复通知", "纯文本", "<p>HTML</p>")
        );

        verify(mailDeliveryService).markAttempt(1L, MailDeliveryStatus.FAILED,
                "MailPreparationException", "邮件准备失败");
    }

    @Test
    void shouldNotSendOrOverwriteRecipientWhenSourceValidationFails() {
        sender.submit(MailDelivery.builder().id(1L).build(), () -> {
            throw new IllegalStateException("订阅已关闭");
        });

        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(mailDeliveryService, never()).updateRecipient(any(), any());
        verify(mailDeliveryService).markAttempt(1L, MailDeliveryStatus.FAILED,
                "IllegalStateException", "邮件发送过程中发生运行时错误");
    }
}
