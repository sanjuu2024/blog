package com.ccsanjuu.blog.modules.message.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.message.model.dto.CreateMessageReplyRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.CreateMessageRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessageBatchApprovalRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessageModerationRequestDTO;
import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.modules.message.model.enums.MessageModerationAction;
import com.ccsanjuu.blog.modules.message.model.enums.MessageStatus;
import com.ccsanjuu.blog.modules.message.service.MessageReplyNotificationService;
import com.ccsanjuu.blog.modules.message.support.MessageRateLimiter;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class MessageServiceImplTest {

    private static final Long MESSAGE_ID = 90001L;
    private static final Long USER_ID = 10001L;
    private static final Long ADMIN_ID = 10002L;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MessageRateLimiter messageRateLimiter;

    @Mock
    private MessageReplyNotificationService notificationService;

    private MessageServiceImpl messageService;

    @BeforeAll
    static void initTableInfo() {
        if (com.baomidou.mybatisplus.core.metadata.TableInfoHelper.getTableInfo(Message.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            assistant.setCurrentNamespace(Message.class.getName());
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, Message.class);
        }
    }

    @BeforeEach
    void setUp() {
        messageService = new MessageServiceImpl(messageMapper, userMapper, messageRateLimiter, notificationService);
        ReflectionTestUtils.setField(messageService, "baseMapper", messageMapper);
        ReflectionTestUtils.setField(messageService, "entityClass", Message.class);
        ReflectionTestUtils.setField(messageService, "mapperClass", MessageMapper.class);
    }

    @Test
    void guestMessageShouldBePendingAndUseIpRateLimit() {
        Message message = Message.builder()
                .id(MESSAGE_ID)
                .nickname("访客")
                .email("guest@example.com")
                .content("留言内容")
                .status(MessageStatus.PENDING)
                .notifyOnReply(true)
                .build();
        when(messageMapper.insert(any(Message.class))).thenAnswer(invocation -> {
            Message inserted = invocation.getArgument(0);
            inserted.setId(MESSAGE_ID);
            return 1;
        });
        when(messageMapper.selectById(MESSAGE_ID)).thenReturn(message);

        messageService.createMessage(null, "127.0.0.1", CreateMessageRequestDTO.builder()
                .nickname("访客")
                .email("guest@example.com")
                .content("留言内容")
                .notifyOnReply(true)
                .build());

        verify(messageRateLimiter).acquire("ip:127.0.0.1");
        verify(messageMapper).insert(any(Message.class));
    }

    @Test
    void loggedInMessageShouldUseAccountEmailWhenNotificationIsEnabled() {
        User user = User.builder()
                .id(USER_ID)
                .nickname("注册用户")
                .email("user@example.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        Message stored = Message.builder()
                .id(MESSAGE_ID)
                .userId(USER_ID)
                .email("user@example.com")
                .content("留言内容")
                .status(MessageStatus.PENDING)
                .notifyOnReply(true)
                .build();
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(messageMapper.insert(any(Message.class))).thenAnswer(invocation -> {
            Message inserted = invocation.getArgument(0);
            inserted.setId(MESSAGE_ID);
            return 1;
        });
        when(messageMapper.selectById(MESSAGE_ID)).thenReturn(stored);

        MessageStatus status = messageService.createMessage(
                USER_ID,
                "127.0.0.1",
                CreateMessageRequestDTO.builder()
                        .content("留言内容")
                        .nickname("a".repeat(21))
                        .email("not-an-email")
                        .notifyOnReply(true)
                        .build()
        ).getStatus();

        assertEquals(MessageStatus.PENDING, status);
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(captor.capture());
        assertEquals("user@example.com", captor.getValue().getEmail());
        assertEquals(true, captor.getValue().getNotifyOnReply());
        verify(messageRateLimiter).acquire("user:" + USER_ID);
    }

    @Test
    void guestMessageShouldRejectInvalidEmail() {
        BizException exception = assertThrows(BizException.class,
                () -> messageService.createMessage(null, "127.0.0.1", CreateMessageRequestDTO.builder()
                        .nickname("访客")
                        .email("not-an-email")
                        .content("留言内容")
                        .build()));

        assertEquals(ResultCode.PARAM_INVALID, exception.getResultCode());
        verify(messageMapper, never()).insert(any(Message.class));
    }

    @Test
    void adminMessageShouldBeApprovedWithoutRateLimit() {
        User admin = User.builder()
                .id(ADMIN_ID)
                .nickname("站长")
                .email("admin@example.com")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        when(userMapper.selectById(ADMIN_ID)).thenReturn(admin);
        when(messageMapper.insert(any(Message.class))).thenAnswer(invocation -> {
            Message inserted = invocation.getArgument(0);
            inserted.setId(MESSAGE_ID);
            return 1;
        });
        when(messageMapper.selectById(MESSAGE_ID)).thenAnswer(invocation -> Message.builder()
                .id(MESSAGE_ID)
                .userId(ADMIN_ID)
                .nickname("站长")
                .content("管理员留言")
                .status(MessageStatus.APPROVED)
                .build());

        MessageStatus status = messageService.createMessage(
                ADMIN_ID,
                "127.0.0.1",
                CreateMessageRequestDTO.builder().content("管理员留言").build()
        ).getStatus();

        assertEquals(MessageStatus.APPROVED, status);
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(captor.capture());
        assertEquals(MessageStatus.APPROVED, captor.getValue().getStatus());
        verify(messageRateLimiter, never()).acquire(any());
    }

    @Test
    void notifyWithoutGuestEmailShouldBeRejected() {
        BizException exception = assertThrows(BizException.class,
                () -> messageService.createMessage(null, "127.0.0.1", CreateMessageRequestDTO.builder()
                        .nickname("访客")
                        .content("留言内容")
                        .notifyOnReply(true)
                        .build()));

        assertEquals(ResultCode.MESSAGE_EMAIL_REQUIRED, exception.getResultCode());
        verify(messageMapper, never()).insert(any(Message.class));
    }

    @Test
    void blankModerationReasonShouldBeRejected() {
        Message current = Message.builder().id(MESSAGE_ID).status(MessageStatus.APPROVED).build();
        User admin = User.builder().id(ADMIN_ID).role(UserRole.ADMIN).status(UserStatus.ACTIVE).build();
        when(userMapper.selectById(ADMIN_ID)).thenReturn(admin);
        when(messageMapper.selectById(MESSAGE_ID)).thenReturn(current);
        when(messageMapper.selectByIdForUpdate(MESSAGE_ID)).thenReturn(current);

        BizException exception = assertThrows(BizException.class,
                () -> messageService.moderateMessage(MESSAGE_ID, ADMIN_ID,
                        MessageModerationRequestDTO.builder()
                                .action(MessageModerationAction.HIDE)
                                .reason(" ")
                                .build()));

        assertEquals(ResultCode.MESSAGE_MODERATION_REASON_REQUIRED, exception.getResultCode());
    }

    @Test
    void batchApprovalShouldRejectMoreThanOneHundredMessages() {
        User admin = User.builder().id(ADMIN_ID).role(UserRole.ADMIN).status(UserStatus.ACTIVE).build();
        when(userMapper.selectById(ADMIN_ID)).thenReturn(admin);
        MessageBatchApprovalRequestDTO requestDTO = new MessageBatchApprovalRequestDTO();
        requestDTO.setMessageIds(java.util.stream.LongStream.rangeClosed(1, 101).boxed().toList());

        BizException exception = assertThrows(BizException.class,
                () -> messageService.approveMessages(ADMIN_ID, requestDTO));

        assertEquals(ResultCode.MESSAGE_BATCH_TOO_LARGE, exception.getResultCode());
        verify(messageMapper, never()).selectPendingRootsForUpdate(any());
    }

    @Test
    void batchApprovalShouldRejectDuplicateIds() {
        User admin = User.builder().id(ADMIN_ID).role(UserRole.ADMIN).status(UserStatus.ACTIVE).build();
        when(userMapper.selectById(ADMIN_ID)).thenReturn(admin);
        MessageBatchApprovalRequestDTO requestDTO = new MessageBatchApprovalRequestDTO();
        requestDTO.setMessageIds(List.of(MESSAGE_ID, MESSAGE_ID));

        BizException exception = assertThrows(BizException.class,
                () -> messageService.approveMessages(ADMIN_ID, requestDTO));

        assertEquals(ResultCode.MESSAGE_BATCH_INVALID, exception.getResultCode());
        verify(messageMapper, never()).selectPendingRootsForUpdate(any());
    }

    @Test
    void deletedMessageShouldNotBeModeratedAgain() {
        Message deleted = Message.builder().id(MESSAGE_ID).status(MessageStatus.DELETED).build();
        User admin = User.builder().id(ADMIN_ID).role(UserRole.ADMIN).status(UserStatus.ACTIVE).build();
        when(userMapper.selectById(ADMIN_ID)).thenReturn(admin);
        when(messageMapper.selectById(MESSAGE_ID)).thenReturn(deleted);
        when(messageMapper.selectByIdForUpdate(MESSAGE_ID)).thenReturn(deleted);

        BizException exception = assertThrows(BizException.class,
                () -> messageService.moderateMessage(MESSAGE_ID, ADMIN_ID,
                        MessageModerationRequestDTO.builder()
                                .action(MessageModerationAction.APPROVE)
                                .build()));

        assertEquals(ResultCode.MESSAGE_STATUS_TRANSITION_INVALID, exception.getResultCode());
        verify(messageMapper, never()).update(any(), any());
    }

    @Test
    void userShouldOnlyDeleteOwnRootMessage() {
        Message otherMessage = Message.builder()
                .id(MESSAGE_ID)
                .userId(ADMIN_ID)
                .status(MessageStatus.APPROVED)
                .build();
        when(messageMapper.selectById(MESSAGE_ID)).thenReturn(otherMessage);

        BizException exception = assertThrows(BizException.class,
                () -> messageService.deleteOwnMessage(MESSAGE_ID, USER_ID));

        assertEquals(ResultCode.MESSAGE_NO_PERMISSION, exception.getResultCode());
        verify(messageMapper, never()).selectSubtreeIds(any());
    }

    @Test
    void ownMessageDeletedWhileWaitingForLockShouldReturnNotFound() {
        Message current = Message.builder()
                .id(MESSAGE_ID)
                .userId(USER_ID)
                .status(MessageStatus.APPROVED)
                .build();
        Message deleted = Message.builder()
                .id(MESSAGE_ID)
                .userId(USER_ID)
                .status(MessageStatus.DELETED)
                .build();
        when(messageMapper.selectById(MESSAGE_ID)).thenReturn(current);
        when(messageMapper.selectByIdForUpdate(MESSAGE_ID)).thenReturn(deleted);

        BizException exception = assertThrows(BizException.class,
                () -> messageService.deleteOwnMessage(MESSAGE_ID, USER_ID));

        assertEquals(ResultCode.MESSAGE_NOT_FOUND, exception.getResultCode());
        verify(messageMapper, never()).selectSubtreeIds(any());
    }

    @Test
    void adminReplyShouldBeApprovedAndScheduleNotificationAfterCommit() {
        User admin = User.builder()
                .id(ADMIN_ID)
                .nickname("站长")
                .username("admin")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        Message root = Message.builder()
                .id(MESSAGE_ID)
                .nickname("访客")
                .email("guest@example.com")
                .content("留言")
                .status(MessageStatus.APPROVED)
                .notifyOnReply(true)
                .unsubscribeToken("token")
                .build();
        when(userMapper.selectById(ADMIN_ID)).thenReturn(admin);
        when(messageMapper.selectByIdForUpdate(MESSAGE_ID)).thenReturn(root);
        when(messageMapper.insert(any(Message.class))).thenAnswer(invocation -> {
            Message inserted = invocation.getArgument(0);
            inserted.setId(90002L);
            return 1;
        });
        when(messageMapper.selectById(90002L)).thenAnswer(invocation -> Message.builder()
                .id(90002L)
                .parentId(MESSAGE_ID)
                .nickname("站长")
                .content("感谢留言")
                .status(MessageStatus.APPROVED)
                .build());

        CreateMessageReplyRequestDTO requestDTO = new CreateMessageReplyRequestDTO();
        requestDTO.setContent("感谢留言");

        MessageStatus status = messageService.replyMessage(
                MESSAGE_ID,
                ADMIN_ID,
                requestDTO
        ).getStatus();

        assertEquals(MessageStatus.APPROVED, status);
        verify(notificationService).sendAfterCommit(eq(root), any(Message.class));
    }
}
