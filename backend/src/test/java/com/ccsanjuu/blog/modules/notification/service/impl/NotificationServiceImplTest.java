package com.ccsanjuu.blog.modules.notification.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.comment.service.CommentStickerService;
import com.ccsanjuu.blog.modules.comment.service.impl.CommentStickerServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ccsanjuu.blog.modules.notification.mapper.NotificationMapper;
import com.ccsanjuu.blog.modules.notification.mapper.NotificationRecipientMapper;
import com.ccsanjuu.blog.modules.notification.model.bo.NotificationItemBO;
import com.ccsanjuu.blog.modules.notification.model.bo.NotificationUnreadCountBO;
import com.ccsanjuu.blog.modules.notification.model.dto.CreateAdminNotificationRequestDTO;
import com.ccsanjuu.blog.modules.notification.model.dto.NotificationQueryDTO;
import com.ccsanjuu.blog.modules.notification.model.dto.UpdateAdminNotificationRequestDTO;
import com.ccsanjuu.blog.modules.notification.model.entity.Notification;
import com.ccsanjuu.blog.modules.notification.model.entity.NotificationRecipient;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationStatus;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationTargetScope;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationType;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationRecipientMapper notificationRecipientMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldReturnArticleTitleOnlyForCommentReplyNotifications() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, notificationRecipientMapper, userMapper, transactionManager, stickerService());
        NotificationItemBO commentReply = new NotificationItemBO();
        commentReply.setType(NotificationType.COMMENT_REPLY);
        commentReply.setArticleId(40001L);
        commentReply.setArticleTitle("Vue 与 Spring Boot");
        NotificationItemBO messageReply = new NotificationItemBO();
        messageReply.setType(NotificationType.MESSAGE_REPLY);
        NotificationItemBO adminMessage = new NotificationItemBO();
        adminMessage.setType(NotificationType.ADMIN_MESSAGE);
        when(notificationMapper.selectUserPage(any(), any(), any(), any())).thenAnswer(invocation -> {
            Page<NotificationItemBO> page = invocation.getArgument(0);
            page.setTotal(3);
            page.setRecords(List.of(commentReply, messageReply, adminMessage));
            return page;
        });

        var records = service.getUserNotifications(10001L, new NotificationQueryDTO()).getRecords();

        assertEquals("Vue 与 Spring Boot", records.getFirst().getArticleTitle());
        assertNull(records.get(1).getArticleTitle());
        assertNull(records.get(2).getArticleTitle());
    }

    @Test
    void shouldCreateAdminMessageForAllUsers() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, notificationRecipientMapper, userMapper, transactionManager, stickerService());
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(70001L);
            return 1;
        });
        when(notificationMapper.selectNotificationByIdForUpdate(70001L)).thenAnswer(invocation -> {
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationMapper).insert(captor.capture());
            return captor.getValue();
        });

        CreateAdminNotificationRequestDTO request = new CreateAdminNotificationRequestDTO();
        request.setTargetScope(NotificationTargetScope.ALL_USERS);
        request.setTitle("系统消息");
        request.setContent("内容");
        when(userMapper.selectObjs(any())).thenReturn(List.of(10002L));

        var result = service.createAdminMessage(10001L, request);

        assertEquals(70001L, result.getId());
        verify(notificationRecipientMapper, org.mockito.Mockito.atLeastOnce()).insert(any(NotificationRecipient.class));
    }

    @Test
    void shouldCreateDraftWithoutRecipientsAndSendWhenPublished() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, notificationRecipientMapper, userMapper, transactionManager, stickerService());
        Notification draft = Notification.builder()
                .id(70002L)
                .type(NotificationType.ADMIN_MESSAGE)
                .targetScope(NotificationTargetScope.ALL_USERS)
                .createdBy(10001L)
                .status(NotificationStatus.DRAFT)
                .build();
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(invocation -> {
            ((Notification) invocation.getArgument(0)).setId(70002L);
            return 1;
        });
        when(notificationMapper.selectNotificationByIdForUpdate(70002L)).thenReturn(draft);
        when(notificationMapper.updateById(any(Notification.class))).thenReturn(1);

        CreateAdminNotificationRequestDTO request = new CreateAdminNotificationRequestDTO();
        request.setTargetScope(NotificationTargetScope.ALL_USERS);
        request.setStatus(NotificationStatus.DRAFT);
        request.setTitle("草稿");
        request.setContent("正文");
        when(userMapper.selectObjs(any())).thenReturn(List.of(10002L));
        service.createAdminMessage(10001L, request);

        verify(notificationRecipientMapper, never()).insert(any(NotificationRecipient.class));
        service.updateAdminMessageStatus(70002L, "PUBLISHED");
        verify(notificationRecipientMapper, atLeastOnce()).insert(any(NotificationRecipient.class));
    }

    @Test
    void shouldCreateSelectedDraftWithoutRecipientsUntilPublished() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, notificationRecipientMapper, userMapper, transactionManager, stickerService());
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(70003L);
            return 1;
        });
        when(notificationMapper.selectNotificationByIdForUpdate(70003L)).thenAnswer(invocation -> {
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationMapper).insert(captor.capture());
            return captor.getValue();
        });

        CreateAdminNotificationRequestDTO request = new CreateAdminNotificationRequestDTO();
        request.setTargetScope(NotificationTargetScope.SELECTED_USERS);
        request.setUserId(10002L);
        request.setStatus(NotificationStatus.DRAFT);
        request.setTitle("草稿");
        request.setContent("正文");
        when(userMapper.selectById(10002L)).thenReturn(User.builder().id(10002L).build());

        var result = service.createAdminMessage(10001L, request);

        assertEquals(10002L, result.getUserId());
        verify(notificationRecipientMapper, never()).insert(any(NotificationRecipient.class));
    }

    @Test
    void shouldRejectSelectedUserWhenInsertDoesNotCreateRecipient() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, notificationRecipientMapper, userMapper, transactionManager, stickerService());
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(invocation -> {
            ((Notification) invocation.getArgument(0)).setId(70003L);
            return 1;
        });
        when(notificationRecipientMapper.insert(any(NotificationRecipient.class))).thenReturn(0);
        CreateAdminNotificationRequestDTO request = new CreateAdminNotificationRequestDTO();
        request.setTargetScope(NotificationTargetScope.SELECTED_USERS);
        request.setUserId(10002L);
        request.setTitle("系统消息");
        request.setContent("正文");
        when(userMapper.selectById(10002L)).thenReturn(User.builder().id(10002L).build());

        BizException error = assertThrows(BizException.class, () -> service.createAdminMessage(10001L, request));
        assertEquals(ResultCode.PARAM_INVALID, error.getResultCode());
    }

    @ParameterizedTest
    @CsvSource({"DRAFT, false", "DRAFT, true", "PUBLISHED, false", "PUBLISHED, true"})
    void shouldRejectInvalidRecipientBeforeCreatingMessage(NotificationStatus status, boolean deleted) {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, notificationRecipientMapper, userMapper, transactionManager, stickerService());
        when(userMapper.selectById(10002L)).thenReturn(deleted
                ? User.builder().id(10002L).deletedAt(OffsetDateTime.now()).build() : null);
        CreateAdminNotificationRequestDTO request = new CreateAdminNotificationRequestDTO();
        request.setTargetScope(NotificationTargetScope.SELECTED_USERS);
        request.setUserId(10002L);
        request.setTitle("系统消息");
        request.setContent("正文");
        request.setStatus(status);

        BizException error = assertThrows(BizException.class, () -> service.createAdminMessage(10001L, request));

        assertEquals(ResultCode.PARAM_INVALID, error.getResultCode());
        assertEquals(deleted ? "指定用户已注销，无法接收管理员消息" : "指定用户不存在", error.getMessage());
        verify(notificationMapper, never()).insert(any(Notification.class));
        verify(notificationRecipientMapper, never()).insert(any(NotificationRecipient.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldKeepDraftWhenUpdatedRecipientIsInvalid(boolean deleted) {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, notificationRecipientMapper, userMapper, transactionManager, stickerService());
        Notification draft = Notification.builder()
                .id(70002L)
                .type(NotificationType.ADMIN_MESSAGE)
                .targetScope(NotificationTargetScope.ALL_USERS)
                .title("原标题")
                .status(NotificationStatus.DRAFT)
                .build();
        when(notificationMapper.selectNotificationByIdForUpdate(70002L)).thenReturn(draft);
        when(userMapper.selectById(10002L)).thenReturn(deleted
                ? User.builder().id(10002L).deletedAt(OffsetDateTime.now()).build() : null);
        UpdateAdminNotificationRequestDTO request = new UpdateAdminNotificationRequestDTO();
        request.setTargetScope(NotificationTargetScope.SELECTED_USERS);
        request.setUserId(10002L);
        request.setTitle("新标题");
        request.setContent("正文");

        BizException error = assertThrows(BizException.class, () -> service.updateAdminMessage(70002L, request));

        assertEquals(ResultCode.PARAM_INVALID, error.getResultCode());
        assertEquals(deleted ? "指定用户已注销，无法接收管理员消息" : "指定用户不存在", error.getMessage());
        assertEquals("原标题", draft.getTitle());
        assertEquals(NotificationTargetScope.ALL_USERS, draft.getTargetScope());
        verify(notificationMapper, never()).updateById(any(Notification.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldRevalidateRecipientBeforePublishingDraft(boolean deleted) {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, notificationRecipientMapper, userMapper, transactionManager, stickerService());
        Notification draft = Notification.builder()
                .id(70002L)
                .type(NotificationType.ADMIN_MESSAGE)
                .targetScope(NotificationTargetScope.SELECTED_USERS)
                .selectedUserId(10002L)
                .createdBy(10001L)
                .status(NotificationStatus.DRAFT)
                .build();
        when(notificationMapper.selectNotificationByIdForUpdate(70002L)).thenReturn(draft);
        when(userMapper.selectById(10002L)).thenReturn(deleted
                ? User.builder().id(10002L).deletedAt(OffsetDateTime.now()).build() : null);

        BizException error = assertThrows(BizException.class, () -> service.updateAdminMessageStatus(70002L, "PUBLISHED"));

        assertEquals(ResultCode.PARAM_INVALID, error.getResultCode());
        assertEquals(deleted ? "指定用户已注销，无法接收管理员消息" : "指定用户不存在", error.getMessage());
        assertEquals(NotificationStatus.DRAFT, draft.getStatus());
        verify(notificationMapper, never()).updateById(any(Notification.class));
        verify(notificationRecipientMapper, never()).insert(any(NotificationRecipient.class));
    }

    @Test
    void shouldRejectUnknownReadCategory() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, notificationRecipientMapper, userMapper, transactionManager, stickerService());

        BizException error = assertThrows(BizException.class, () -> service.markAllRead(10001L, "UNKNOWN"));

        assertEquals(ResultCode.PARAM_INVALID, error.getResultCode());
        verify(notificationRecipientMapper, never()).selectIdsForRead(any(), any());
    }

    @Test
    void shouldPersistReplySource() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, notificationRecipientMapper, userMapper, transactionManager, stickerService());
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(invocation -> {
            ((Notification) invocation.getArgument(0)).setId(70004L);
            return 1;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.createCommentReplyNotification(10002L, 30001L, "回复", "内容");
            verify(notificationMapper, never()).insert(any(Notification.class));
            TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals("COMMENT", captor.getValue().getSourceType());
        assertEquals(30001L, captor.getValue().getSourceId());
    }

    @Test
    void commentStickerTextShouldRespectExistingNotificationLengthLimit() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, notificationRecipientMapper,
                userMapper, transactionManager, stickerService());
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service.createCommentReplyNotification(10002L, 30001L, "回复", "[sticker:bilibili:doge]".repeat(1000));
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals(2000, captor.getValue().getContent().length());
        assertEquals("[doge]".repeat(333) + "[…", captor.getValue().getContent());
    }

    @Test
    void shouldNotPropagateReplyNotificationFailureAfterCommit() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, notificationRecipientMapper, userMapper, transactionManager, stickerService());
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
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, notificationRecipientMapper, userMapper, transactionManager, stickerService());
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

    /**
     * 读取真实目录，避免文字转换测试依赖模拟的表情结果。
     *
     * @return 表情服务
     */
    private CommentStickerService stickerService() {
        try {
            return new CommentStickerServiceImpl(new ObjectMapper());
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
