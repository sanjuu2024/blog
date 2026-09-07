package com.ccsanjuu.blog.modules.message.service.impl;

import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.properties.BlogMailProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageReplyNotificationServiceImplTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    private BlogMailProperties mailProperties;
    private MessageReplyNotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        mailProperties = new BlogMailProperties();
        mailProperties.setEnabled(true);
        mailProperties.setFrom("noreply@example.com");
        mailProperties.setFrontendBaseUrl("https://blog.example.com");
        TaskExecutor directExecutor = Runnable::run;
        notificationService = new MessageReplyNotificationServiceImpl(
                mailSenderProvider,
                mailProperties,
                directExecutor
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
    }

    @Test
    void temporaryMailFailureShouldRetry() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new MailSendException("temporary network failure"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.sendAfterCommit(rootMessage(), replyMessage());

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
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
                rejectingExecutor
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
                rejectingExecutor
        );

        assertDoesNotThrow(() -> notificationService.sendAfterCommit(rootMessage(), replyMessage()));
        verify(mailSenderProvider, never()).getIfAvailable();
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
