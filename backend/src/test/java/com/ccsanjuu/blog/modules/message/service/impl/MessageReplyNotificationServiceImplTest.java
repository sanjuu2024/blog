package com.ccsanjuu.blog.modules.message.service.impl;

import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.modules.mail.service.MailDeliveryService;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import com.ccsanjuu.blog.modules.mail.model.enums.MailDeliveryStatus;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.properties.BlogMailProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class MessageReplyNotificationServiceImplTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MailDeliveryService mailDeliveryService;

    @Mock
    private MessageMapper messageMapper;

    private BlogMailProperties mailProperties;
    private MessageReplyNotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        mailProperties = new BlogMailProperties();
        mailProperties.setEnabled(true);
        mailProperties.setFrom("noreply@example.com");
        mailProperties.setFrontendBaseUrl("https://blog.example.com");
        lenient().when(mailDeliveryService.createPending(any(), any(), any(), any()))
                .thenReturn(MailDelivery.builder().id(1L).build());
        TaskExecutor directExecutor = Runnable::run;
        notificationService = new MessageReplyNotificationServiceImpl(
                mailSenderProvider,
                mailProperties,
                directExecutor,
                mailDeliveryService,
                messageMapper
        );
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void notificationShouldOnlyBeSentAfterTransactionCommit() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        TransactionSynchronizationManager.initSynchronization();

        notificationService.sendAfterCommit(rootMessage(), replyMessage());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getText().contains("管理员回复内容"));
        assertTrue(captor.getValue().getText().contains("/messages/unsubscribe?token=unsubscribe-token"));
    }

    @Test
    void permanentMailFailureShouldNotRetry() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new MailAuthenticationException("authentication failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.sendAfterCommit(rootMessage(), replyMessage());

        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(mailDeliveryService).markAttempt(
                1L, MailDeliveryStatus.FAILED, "MailAuthenticationException", "SMTP 认证失败");
    }

    @Test
    void temporaryMailFailureShouldRetry() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new MailSendException("temporary network failure"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.sendAfterCommit(rootMessage(), replyMessage());

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        verify(mailDeliveryService).markAttempt(
                1L, MailDeliveryStatus.PENDING, "MailSendException", "SMTP 发送失败");
        verify(mailDeliveryService).markSent(1L);
    }

    @Test
    void runtimeMailFailureShouldBeRecordedAsFailed() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new IllegalStateException("unexpected failure"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.sendAfterCommit(rootMessage(), replyMessage());

        verify(mailDeliveryService).markAttempt(
                1L, MailDeliveryStatus.FAILED, "IllegalStateException", "邮件发送过程中发生运行时错误");
    }

    @Test
    void unavailableMailSenderShouldBeLoggedWithoutFailingCommittedReply(CapturedOutput output) {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        assertDoesNotThrow(() -> notificationService.sendAfterCommit(rootMessage(), replyMessage()));

        assertTrue(output.getOut().contains("notification_event=MESSAGE_REPLY_MAIL_FAILED"));
        assertTrue(output.getOut().contains("reason=MAIL_SENDER_UNAVAILABLE"));
        assertTrue(output.getOut().contains("messageId=90001"));
        assertTrue(output.getOut().contains("replyId=90002"));
        assertFalse(output.getOut().contains("guest@example.com"));
        assertFalse(output.getOut().contains("留言内容"));
    }

    @Test
    void disabledNotificationShouldNotResolveMailSender() {
        Message root = rootMessage();
        root.setNotifyOnReply(false);

        notificationService.sendAfterCommit(root, replyMessage());

        verify(mailSenderProvider, never()).getIfAvailable();
    }

    @Test
    void disabledMailFeatureShouldNotSubmitTask() {
        mailProperties.setEnabled(false);
        TaskExecutor rejectingExecutor = task -> {
            throw new IllegalStateException("task should not be submitted");
        };
        notificationService = new MessageReplyNotificationServiceImpl(
                mailSenderProvider,
                mailProperties,
                rejectingExecutor,
                mailDeliveryService,
                messageMapper
        );

        assertDoesNotThrow(() -> notificationService.sendAfterCommit(rootMessage(), replyMessage()));
        verify(mailSenderProvider, never()).getIfAvailable();
    }

    @Test
    void rejectedMailTaskShouldNotFailCommittedReply() {
        TaskExecutor rejectingExecutor = task -> {
            throw new IllegalStateException("executor stopped");
        };
        notificationService = new MessageReplyNotificationServiceImpl(
                mailSenderProvider,
                mailProperties,
                rejectingExecutor,
                mailDeliveryService,
                messageMapper
        );

        assertDoesNotThrow(() -> notificationService.sendAfterCommit(rootMessage(), replyMessage()));
        verify(mailSenderProvider, never()).getIfAvailable();
        verify(mailDeliveryService).markAttempt(
                1L, MailDeliveryStatus.FAILED, "IllegalStateException", "邮件发送任务提交失败");
    }

    private Message rootMessage() {
        return Message.builder()
                .id(90001L)
                .email("guest@example.com")
                .content("留言内容")
                .notifyOnReply(true)
                .unsubscribeToken("unsubscribe-token")
                .build();
    }

    private Message replyMessage() {
        return Message.builder()
                .id(90002L)
                .parentId(90001L)
                .content("管理员回复内容")
                .build();
    }
}
