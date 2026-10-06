package com.ccsanjuu.blog.modules.message.service.impl;

import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import com.ccsanjuu.blog.modules.mail.service.MailDeliveryService;
import com.ccsanjuu.blog.modules.mail.service.MailNotificationSender;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.properties.BlogProperties;
import com.ccsanjuu.blog.properties.BlogMailProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageReplyNotificationServiceImplTest {

    @Mock
    private MailDeliveryService mailDeliveryService;

    @Mock
    private MailNotificationSender mailNotificationSender;

    @Mock
    private MessageMapper messageMapper;

    private MessageReplyNotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        BlogMailProperties properties = new BlogMailProperties();
        properties.setEnabled(true);
        properties.setFrontendBaseUrl("https://blog.example.com");
        org.mockito.Mockito.lenient().when(mailDeliveryService.createPending(any(), any(), any(), any()))
                .thenReturn(MailDelivery.builder().id(1L).build());
        notificationService = new MessageReplyNotificationServiceImpl(
                properties, new BlogProperties(), mailDeliveryService, mailNotificationSender, messageMapper);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void notificationShouldOnlyBeSubmittedAfterTransactionCommit() {
        TransactionSynchronizationManager.initSynchronization();
        notificationService.sendAfterCommit(rootMessage(), replyMessage());

        verify(mailNotificationSender, never()).submit(any(), any());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(mailNotificationSender).submit(any(), any());
    }

    @Test
    void disabledNotificationShouldNotCreateDelivery() {
        Message root = rootMessage();
        root.setNotifyOnReply(false);

        notificationService.sendAfterCommit(root, replyMessage());

        verify(mailDeliveryService, never()).createPending(any(), any(), any(), any());
        verify(mailNotificationSender, never()).submit(any(), any());
    }

    @Test
    void retryShouldSubmitExistingDelivery() {
        notificationService.retry(MailDelivery.builder().id(1L).sourceId(90001L).replyId(90002L).build());

        verify(mailNotificationSender).submit(any(), any());
    }

    private Message rootMessage() {
        return Message.builder().id(90001L).email("guest@example.com").content("留言内容")
                .notifyOnReply(true).unsubscribeToken("unsubscribe-token").build();
    }

    private Message replyMessage() {
        return Message.builder().id(90002L).parentId(90001L).content("管理员回复内容").build();
    }
}
