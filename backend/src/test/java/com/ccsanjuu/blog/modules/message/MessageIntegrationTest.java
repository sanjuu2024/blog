package com.ccsanjuu.blog.modules.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.message.model.dto.CreateMessageReplyRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.CreateMessageRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessageBatchApprovalRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessageModerationRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessagePageQueryDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessageUnsubscribeRequestDTO;
import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.modules.message.model.enums.MessageModerationAction;
import com.ccsanjuu.blog.modules.message.model.enums.MessageStatus;
import com.ccsanjuu.blog.modules.message.model.vo.MessageMutationVO;
import com.ccsanjuu.blog.modules.message.model.vo.PublicMessageItemVO;
import com.ccsanjuu.blog.modules.message.service.MessageService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MessageIntegrationTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void guestMessageShouldUseRealDatabaseAndRedisRateLimit() {
        String clientIp = "integration-guest-" + System.nanoTime();
        MessageMutationVO created = messageService.createMessage(
                null,
                clientIp,
                CreateMessageRequestDTO.builder()
                        .nickname("测试访客")
                        .email("guest@example.com")
                        .notifyOnReply(true)
                        .content("这是一条留言集成测试")
                        .build()
        );

        assertEquals(MessageStatus.PENDING, created.getStatus());
        assertTrue(messageMapper.selectById(created.getId()).getNotifyOnReply());

        BizException exception = assertThrows(BizException.class, () -> messageService.createMessage(
                null,
                clientIp,
                CreateMessageRequestDTO.builder()
                        .nickname("测试访客")
                        .content("第二条留言")
                        .build()
        ));
        assertEquals(ResultCode.MESSAGE_RATE_LIMITED, exception.getResultCode());
    }

    @Test
    void unsubscribeShouldBeIdempotentAndDisableFutureNotification() {
        MessageMutationVO created = messageService.createMessage(
                null,
                "integration-unsubscribe-" + System.nanoTime(),
                CreateMessageRequestDTO.builder()
                        .nickname("测试访客")
                        .email("guest@example.com")
                        .notifyOnReply(true)
                        .content("准备退订通知")
                        .build()
        );
        Message stored = messageMapper.selectById(created.getId());

        MessageUnsubscribeRequestDTO requestDTO = new MessageUnsubscribeRequestDTO();
        requestDTO.setToken(stored.getUnsubscribeToken());
        messageService.unsubscribe(requestDTO);
        messageService.unsubscribe(requestDTO);

        assertEquals(false, messageMapper.selectById(created.getId()).getNotifyOnReply());
    }

    @Test
    void adminReplyShouldOnlyAcceptApprovedRootMessage() {
        User admin = createUser("reply_pending_admin", UserRole.ADMIN);
        Message root = Message.builder()
                .nickname("集成测试访客")
                .email("")
                .content("待审核留言")
                .status(MessageStatus.PENDING)
                .notifyOnReply(false)
                .build();
        messageMapper.insert(root);

        CreateMessageReplyRequestDTO requestDTO = new CreateMessageReplyRequestDTO();
        requestDTO.setContent("不应回复");
        BizException exception = assertThrows(BizException.class, () -> messageService.replyMessage(
                root.getId(),
                admin.getId(),
                requestDTO
        ));

        assertEquals(ResultCode.MESSAGE_PARENT_UNAVAILABLE, exception.getResultCode());
    }

    @Test
    void loggedInUserShouldSeeOwnPendingAndRejectedMessages() {
        User user = createUser("msg_visible_user", UserRole.USER);
        User other = createUser("msg_visible_other", UserRole.USER);
        Message approved = createMessage(null, null, MessageStatus.APPROVED, "已通过留言");
        Message ownPending = createMessage(user.getId(), null, MessageStatus.PENDING, "自己的待审核留言");
        Message ownRejected = createMessage(user.getId(), null, MessageStatus.REJECTED, "自己的已拒绝留言");
        Message otherPending = createMessage(other.getId(), null, MessageStatus.PENDING, "其他人的待审核留言");
        Message ownHidden = createMessage(user.getId(), null, MessageStatus.HIDDEN, "自己的已隐藏留言");

        Set<Long> userVisibleIds = messageService.getPublicMessageList(user.getId(), pageQuery()).getRecords().stream()
                .map(PublicMessageItemVO::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> guestVisibleIds = messageService.getPublicMessageList(null, pageQuery()).getRecords().stream()
                .map(PublicMessageItemVO::getId)
                .collect(java.util.stream.Collectors.toSet());
        MessagePageQueryDTO adminQuery = pageQuery();
        adminQuery.setMessageId(approved.getId());

        assertTrue(userVisibleIds.containsAll(Set.of(approved.getId(), ownPending.getId(), ownRejected.getId())));
        assertFalse(userVisibleIds.contains(otherPending.getId()));
        assertFalse(userVisibleIds.contains(ownHidden.getId()));
        assertTrue(guestVisibleIds.contains(approved.getId()));
        assertFalse(guestVisibleIds.contains(ownPending.getId()));
        assertFalse(guestVisibleIds.contains(ownRejected.getId()));
        assertNull(messageService.getAdminMessageList(adminQuery).getRecords().getFirst().getAuthor());
    }

    @Test
    void batchApprovalShouldUpdateAllPendingRoots() {
        User admin = createUser("msg_batch_admin", UserRole.ADMIN);
        Message first = createMessage(null, null, MessageStatus.PENDING, "第一条待审核留言");
        Message second = createMessage(null, null, MessageStatus.PENDING, "第二条待审核留言");
        MessageBatchApprovalRequestDTO requestDTO = new MessageBatchApprovalRequestDTO();
        requestDTO.setMessageIds(List.of(first.getId(), second.getId()));

        messageService.approveMessages(admin.getId(), requestDTO);

        for (Long id : requestDTO.getMessageIds()) {
            Message approved = messageMapper.selectById(id);
            assertEquals(MessageStatus.APPROVED, approved.getStatus());
            assertEquals(admin.getId(), approved.getReviewedBy());
            assertNotNull(approved.getReviewedAt());
        }
    }

    @Test
    void batchApprovalShouldNotUpdateAnyMessageWhenBatchContainsInvalidStatus() {
        User admin = createUser("msg_batch_atomic", UserRole.ADMIN);
        Message pending = createMessage(null, null, MessageStatus.PENDING, "待审核留言");
        Message approved = createMessage(null, null, MessageStatus.APPROVED, "已通过留言");
        MessageBatchApprovalRequestDTO requestDTO = new MessageBatchApprovalRequestDTO();
        requestDTO.setMessageIds(List.of(pending.getId(), approved.getId()));

        BizException exception = assertThrows(BizException.class,
                () -> messageService.approveMessages(admin.getId(), requestDTO));

        assertEquals(ResultCode.MESSAGE_BATCH_INVALID, exception.getResultCode());
        assertEquals(MessageStatus.PENDING, messageMapper.selectById(pending.getId()).getStatus());
        assertEquals(MessageStatus.APPROVED, messageMapper.selectById(approved.getId()).getStatus());
    }

    @Test
    void guestFiltersShouldNotMatchLoggedInUserSnapshots() {
        User user = createUser("msg_filter_user", UserRole.USER);
        Message guest = createMessage(null, null, MessageStatus.APPROVED, "游客筛选留言");
        guest.setNickname("相同昵称");
        guest.setEmail("same@example.com");
        messageMapper.updateById(guest);
        Message member = createMessage(user.getId(), null, MessageStatus.APPROVED, "登录用户留言");
        member.setNickname("相同昵称");
        member.setEmail("same@example.com");
        messageMapper.updateById(member);
        MessagePageQueryDTO queryDTO = pageQuery();
        queryDTO.setGuestNickname("相同昵称");
        queryDTO.setGuestEmail("same@example.com");

        List<Long> ids = messageService.getAdminMessageList(queryDTO).getRecords().stream()
                .map(item -> item.getId())
                .toList();

        assertEquals(List.of(guest.getId()), ids);
    }

    @Test
    void deletingReplyShouldKeepRootAndDeletingRootShouldDeleteAllReplies() {
        User user = createUser("msg_delete_user", UserRole.USER);
        User admin = createUser("msg_delete_admin", UserRole.ADMIN);
        Message root = createMessage(user.getId(), null, MessageStatus.APPROVED, "等待管理员回复");
        CreateMessageReplyRequestDTO replyDTO = new CreateMessageReplyRequestDTO();
        replyDTO.setContent("管理员回复");

        Long firstReplyId = messageService.replyMessage(root.getId(), admin.getId(), replyDTO).getId();
        Long secondReplyId = messageService.replyMessage(root.getId(), admin.getId(), replyDTO).getId();
        assertEquals(2L, messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getParentId, root.getId())
                .eq(Message::getStatus, MessageStatus.APPROVED)));

        messageService.moderateMessage(firstReplyId, admin.getId(), MessageModerationRequestDTO.builder()
                .action(MessageModerationAction.DELETE)
                .reason("删除单条回复")
                .build());

        assertEquals(MessageStatus.APPROVED, messageMapper.selectById(root.getId()).getStatus());
        assertEquals(MessageStatus.DELETED, messageMapper.selectById(firstReplyId).getStatus());
        assertEquals(MessageStatus.APPROVED, messageMapper.selectById(secondReplyId).getStatus());

        messageService.deleteOwnMessage(root.getId(), user.getId());

        assertEquals(MessageStatus.DELETED, messageMapper.selectById(root.getId()).getStatus());
        assertEquals(MessageStatus.DELETED, messageMapper.selectById(firstReplyId).getStatus());
        assertEquals(MessageStatus.DELETED, messageMapper.selectById(secondReplyId).getStatus());
        assertNotNull(messageMapper.selectById(root.getId()).getDeletedAt());
        assertEquals(user.getId(), messageMapper.selectById(secondReplyId).getDeletedBy());
    }

    private User createUser(String username, UserRole role) {
        User user = User.builder()
                .username(username)
                .nickname(username.substring(0, Math.min(username.length(), 20)))
                .email(username + "@example.com")
                .passwordHash("integration-test-password")
                .role(role)
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .avatarUrl("")
                .bio("")
                .emailVerified(false)
                .build();
        userMapper.insert(user);
        return user;
    }

    private Message createMessage(Long userId, Long parentId, MessageStatus status, String content) {
        Message message = Message.builder()
                .userId(userId)
                .parentId(parentId)
                .nickname(userId == null ? "集成测试访客" : "集成测试用户")
                .email("")
                .content(content)
                .status(status)
                .notifyOnReply(false)
                .build();
        messageMapper.insert(message);
        return message;
    }

    private MessagePageQueryDTO pageQuery() {
        MessagePageQueryDTO queryDTO = new MessagePageQueryDTO();
        queryDTO.setPageSize(100);
        return queryDTO;
    }
}
