package com.ccsanjuu.blog.modules.notification.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.notification.mapper.NotificationMapper;
import com.ccsanjuu.blog.modules.notification.model.bo.NotificationUnreadCountBO;
import com.ccsanjuu.blog.modules.notification.model.dto.CreateAdminNotificationRequestDTO;
import com.ccsanjuu.blog.modules.notification.model.entity.Notification;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationStatus;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationTargetScope;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldCreateAdminMessageForAllActiveUsers() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, transactionManager);
        when(notificationMapper.insertNotification(any())).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(70001L);
            return 1;
        });
        when(notificationMapper.selectNotificationByIdForUpdate(70001L)).thenAnswer(invocation -> {
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationMapper).insertNotification(captor.capture());
            return captor.getValue();
        });

        CreateAdminNotificationRequestDTO request = new CreateAdminNotificationRequestDTO();
        request.setTargetScope(NotificationTargetScope.ALL_USERS);
        request.setTitle("系统消息");
        request.setContent("内容");

        var result = service.createAdminMessage(10001L, request);

        assertEquals(70001L, result.getId());
        verify(notificationMapper).insertRecipientsForAllUsers(70001L);
    }

    @Test
    void shouldCreateDraftWithoutRecipientsAndSendWhenPublished() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, transactionManager);
        Notification draft = Notification.builder()
                .id(70002L)
                .type(NotificationType.ADMIN_MESSAGE)
                .targetScope(NotificationTargetScope.ALL_USERS)
                .status(NotificationStatus.DRAFT)
                .build();
        when(notificationMapper.insertNotification(any())).thenAnswer(invocation -> {
            ((Notification) invocation.getArgument(0)).setId(70002L);
            return 1;
        });
        when(notificationMapper.selectNotificationByIdForUpdate(70002L)).thenReturn(draft);
        when(notificationMapper.updateNotificationStatus(any())).thenReturn(1);

        CreateAdminNotificationRequestDTO request = new CreateAdminNotificationRequestDTO();
        request.setTargetScope(NotificationTargetScope.ALL_USERS);
        request.setStatus(NotificationStatus.DRAFT);
        request.setTitle("草稿");
        request.setContent("正文");
        service.createAdminMessage(10001L, request);

        verify(notificationMapper, never()).insertRecipientsForAllUsers(70002L);
        service.updateAdminMessageStatus(70002L, "PUBLISHED");
        verify(notificationMapper).insertRecipientsForAllUsers(70002L);
    }

    @Test
    void shouldRejectSelectedUsersWhenOneCannotReceive() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, transactionManager);
        when(notificationMapper.insertNotification(any())).thenAnswer(invocation -> {
            ((Notification) invocation.getArgument(0)).setId(70003L);
            return 1;
        });
        when(notificationMapper.insertRecipientsForUsers(70003L, List.of(10002L, 10003L))).thenReturn(1);
        CreateAdminNotificationRequestDTO request = new CreateAdminNotificationRequestDTO();
        request.setTargetScope(NotificationTargetScope.SELECTED_USERS);
        request.setUserIds(List.of(10002L, 10003L));
        request.setTitle("系统消息");
        request.setContent("正文");

        BizException error = assertThrows(BizException.class, () -> service.createAdminMessage(10001L, request));
        assertEquals(ResultCode.PARAM_INVALID, error.getResultCode());
    }

    @Test
    void shouldRejectUnknownReadCategory() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, transactionManager);

        BizException error = assertThrows(BizException.class, () -> service.markAllRead(10001L, "UNKNOWN"));

        assertEquals(ResultCode.PARAM_INVALID, error.getResultCode());
        verify(notificationMapper, never()).markAllRead(any(), any());
    }

    @Test
    void shouldPersistReplySource() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, transactionManager);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(notificationMapper.insertNotification(any())).thenAnswer(invocation -> {
            ((Notification) invocation.getArgument(0)).setId(70004L);
            return 1;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.createCommentReplyNotification(10002L, 30001L, "回复", "内容");
            verify(notificationMapper, never()).insertNotification(any());
            TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insertNotification(captor.capture());
        assertEquals("COMMENT", captor.getValue().getSourceType());
        assertEquals(30001L, captor.getValue().getSourceId());
    }

    @Test
    void shouldNotPropagateReplyNotificationFailureAfterCommit() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, transactionManager);
        when(transactionManager.getTransaction(any())).thenThrow(new IllegalStateException("database unavailable"));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.createMessageReplyNotification(10002L, 90001L, "留言回复", "内容");

            assertDoesNotThrow(() ->
                    TransactionSynchronizationManager.getSynchronizations()
                            .forEach(synchronization -> synchronization.afterCommit()));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldReturnUnreadCounts() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, transactionManager);
        NotificationUnreadCountBO count = new NotificationUnreadCountBO();
        count.setTotal(3L);
        count.setReply(2L);
        count.setAdminMessage(1L);
        when(notificationMapper.selectUnreadCount(10001L)).thenReturn(count);

        var result = service.getUnreadCount(10001L);

        assertEquals(3L, result.getTotal());
        assertEquals(2L, result.getReply());
        assertEquals(1L, result.getAdminMessage());
    }
}
