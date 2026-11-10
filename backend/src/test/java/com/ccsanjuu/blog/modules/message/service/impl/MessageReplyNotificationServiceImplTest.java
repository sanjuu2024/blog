package com.ccsanjuu.blog.modules.message.service.impl;

import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import com.ccsanjuu.blog.modules.mail.model.bo.MailMessageContent;
import com.ccsanjuu.blog.modules.mail.service.MailDeliveryService;
import com.ccsanjuu.blog.modules.mail.service.MailNotificationSender;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.modules.message.model.enums.MessageStatus;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.properties.BlogProperties;
import com.ccsanjuu.blog.properties.BlogMailProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Mock
    private UserMapper userMapper;

    private MessageReplyNotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        BlogMailProperties properties = new BlogMailProperties();
        properties.setEnabled(true);
        properties.setFrontendBaseUrl("https://blog.example.com");
        org.mockito.Mockito.lenient().when(mailDeliveryService.createPending(any(), any(), any(), any(), any()))
                .thenReturn(MailDelivery.builder().id(1L).build());
        notificationService = new MessageReplyNotificationServiceImpl(
                properties, new BlogProperties(), mailDeliveryService, mailNotificationSender, messageMapper, userMapper);
        org.mockito.Mockito.lenient().when(messageMapper.selectById(90001L)).thenReturn(rootMessage());
        org.mockito.Mockito.lenient().when(messageMapper.selectById(90002L)).thenReturn(replyMessage());
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

        verify(mailDeliveryService, never()).createPending(any(), any(), any(), any(), any());
        verify(mailNotificationSender, never()).submit(any(), any());
    }

    @Test
    void retryShouldSubmitExistingDelivery() {
        notificationService.retry(MailDelivery.builder().id(1L).sourceId(90001L).replyId(90002L).build());

        verify(mailNotificationSender).submit(any(), any());
    }

    @Test
    void queuedFirstAttemptShouldRecheckUnsubscribeInsteadOfUsingTheCapturedMessage() {
        notificationService.sendAfterCommit(rootMessage(), replyMessage());
        Message current = rootMessage();
        current.setNotifyOnReply(false);
        when(messageMapper.selectById(90001L)).thenReturn(current);

        ArgumentCaptor<Supplier<MailMessageContent>> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(mailNotificationSender).submit(any(), captor.capture());
        assertThrows(IllegalStateException.class, () -> captor.getValue().get());
    }

    @Test
    void retryShouldRejectHiddenReply() {
        Message reply = replyMessage();
        reply.setStatus(MessageStatus.HIDDEN);
        when(messageMapper.selectById(90002L)).thenReturn(reply);
        notificationService.retry(MailDelivery.builder().id(1L).sourceId(90001L).replyId(90002L).build());

        ArgumentCaptor<Supplier<MailMessageContent>> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(mailNotificationSender).submit(any(), captor.capture());
        assertThrows(IllegalStateException.class, () -> captor.getValue().get());
    }

    @Test
    void retryShouldRejectDeletedRecipient() {
        Message root = rootMessage();
        root.setUserId(10001L);
        when(messageMapper.selectById(90001L)).thenReturn(root);
        when(userMapper.selectById(10001L)).thenReturn(User.builder().id(10001L)
                .emailVerified(true).deletedAt(OffsetDateTime.now()).build());
        notificationService.retry(MailDelivery.builder().id(1L).sourceId(90001L).replyId(90002L).build());

        ArgumentCaptor<Supplier<MailMessageContent>> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(mailNotificationSender).submit(any(), captor.capture());
        assertThrows(IllegalStateException.class, () -> captor.getValue().get());
    }

    @Test
    void retryShouldUseCurrentVisibleContentWhenSubscriptionIsValid() {
        notificationService.retry(MailDelivery.builder().id(1L).sourceId(90001L).replyId(90002L).build());

        ArgumentCaptor<Supplier<MailMessageContent>> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(mailNotificationSender).submit(any(), captor.capture());
        assertTrue(captor.getValue().get().plainText().contains("管理员回复内容"));
    }

    private Message rootMessage() {
        return Message.builder().id(90001L).email("guest@example.com").content("留言内容")
                .status(MessageStatus.APPROVED).notifyOnReply(true).unsubscribeToken("unsubscribe-token").build();
    }

    private Message replyMessage() {
        return Message.builder().id(90002L).parentId(90001L).status(MessageStatus.APPROVED).content("管理员回复内容").build();
    }
}
