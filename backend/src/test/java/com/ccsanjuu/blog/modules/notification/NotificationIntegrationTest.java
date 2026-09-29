package com.ccsanjuu.blog.modules.notification;

import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.message.model.dto.CreateMessageRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessagePageQueryDTO;
import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.modules.message.model.enums.MessageStatus;
import com.ccsanjuu.blog.modules.message.service.MessageService;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import com.ccsanjuu.blog.modules.notification.mapper.NotificationMapper;
import com.ccsanjuu.blog.modules.notification.model.dto.CreateAdminNotificationRequestDTO;
import com.ccsanjuu.blog.modules.notification.model.dto.NotificationQueryDTO;
import com.ccsanjuu.blog.modules.notification.model.entity.Notification;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationStatus;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationTargetScope;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationType;
import com.ccsanjuu.blog.modules.notification.service.NotificationService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void draftShouldBecomeVisibleOnlyAfterPublishingAndSupportReadState() {
        User admin = createUser("notification_admin", UserRole.ADMIN);
        User recipient = createUser("notify_recipient", UserRole.USER);
        User disabledRecipient = createUser("notify_disabled_recipient", UserRole.USER);
        disabledRecipient.setStatus(UserStatus.DISABLED);
        userMapper.updateById(disabledRecipient);
        CreateAdminNotificationRequestDTO request = new CreateAdminNotificationRequestDTO();
        request.setTargetScope(NotificationTargetScope.ALL_USERS);
        request.setTitle("系统消息");
        request.setContent("测试内容");
        request.setStatus(NotificationStatus.DRAFT);

        var draft = notificationService.createAdminMessage(admin.getId(), request);
        assertTrue(recipientUserIds(draft.getId()).isEmpty());
        assertEquals(0L, notificationService.getUnreadCount(recipient.getId()).getTotal());

        notificationService.updateAdminMessageStatus(draft.getId(), "PUBLISHED");
        assertTrue(recipientUserIds(draft.getId()).contains(recipient.getId()));
        assertTrue(recipientUserIds(draft.getId()).contains(disabledRecipient.getId()));
        assertFalse(recipientUserIds(draft.getId()).contains(admin.getId()));
        assertEquals(1L, notificationService.getUnreadCount(recipient.getId()).getTotal());
        notificationService.markAllRead(recipient.getId(), "ADMIN_MESSAGE");
        assertEquals(0L, notificationService.getUnreadCount(recipient.getId()).getTotal());
        NotificationQueryDTO query = new NotificationQueryDTO();
        assertTrue(notificationService.getUserNotifications(recipient.getId(), query).getRecords().getFirst().getRead());

        disabledRecipient.setStatus(UserStatus.ACTIVE);
        userMapper.updateById(disabledRecipient);
        assertEquals(1, notificationService.getUserNotifications(disabledRecipient.getId(), query).getRecords().size());

        notificationService.updateAdminMessageStatus(draft.getId(), "OFFLINE");
        assertTrue(notificationService.getUserNotifications(recipient.getId(), query).getRecords().isEmpty());
    }

    @Test
    void disabledSelectedUserShouldReadNotificationAfterReactivation() {
        User admin = createUser("selected_notification_admin", UserRole.ADMIN);
        User recipient = createUser("selected_disabled_recipient", UserRole.USER);
        recipient.setStatus(UserStatus.DISABLED);
        userMapper.updateById(recipient);

        CreateAdminNotificationRequestDTO request = new CreateAdminNotificationRequestDTO();
        request.setTargetScope(NotificationTargetScope.SELECTED_USERS);
        request.setUserId(recipient.getId());
        request.setTitle("指定消息");
        request.setContent("禁用期间创建的通知");

        var notification = notificationService.createAdminMessage(admin.getId(), request);
        assertTrue(recipientUserIds(notification.getId()).contains(recipient.getId()));

        recipient.setStatus(UserStatus.ACTIVE);
        userMapper.updateById(recipient);
        assertEquals(1, notificationService.getUserNotifications(recipient.getId(), new NotificationQueryDTO())
                .getRecords().size());
    }

    @Test
    void selectedDraftShouldCreateRecipientOnlyWhenPublished() {
        User admin = createUser("selected_draft_admin", UserRole.ADMIN);
        User recipient = createUser("selected_draft_recipient", UserRole.USER);
        CreateAdminNotificationRequestDTO request = new CreateAdminNotificationRequestDTO();
        request.setTargetScope(NotificationTargetScope.SELECTED_USERS);
        request.setUserId(recipient.getId());
        request.setStatus(NotificationStatus.DRAFT);
        request.setTitle("指定用户草稿");
        request.setContent("草稿正文");

        var draft = notificationService.createAdminMessage(admin.getId(), request);
        assertTrue(recipientUserIds(draft.getId()).isEmpty());

        notificationService.updateAdminMessageStatus(draft.getId(), "PUBLISHED");
        assertTrue(recipientUserIds(draft.getId()).contains(recipient.getId()));
    }

    @Test
    void notificationMapperShouldStoreReplySource() {
        Notification notification = Notification.builder()
                .type(NotificationType.COMMENT_REPLY)
                .targetScope(NotificationTargetScope.SELECTED_USERS)
                .title("评论回复")
                .content("内容")
                .status(NotificationStatus.PUBLISHED)
                .sourceType("COMMENT")
                .sourceId(321L)
                .build();
        notificationMapper.insertNotification(notification);

        assertEquals(321L, jdbcTemplate.queryForObject(
                "SELECT source_id FROM blog_notification WHERE id = ?", Long.class, notification.getId()));
        assertEquals("COMMENT", jdbcTemplate.queryForObject(
                "SELECT source_type FROM blog_notification WHERE id = ?", String.class, notification.getId()));
    }

    @Test
    void replyNotificationsShouldIncludeAuthorOriginalContentAndDestination() {
        User recipient = createUser("notify_reader", UserRole.USER);
        User author = createUser("notify_writer", UserRole.ADMIN);
        Comment originalComment = Comment.builder()
                .articleId(40001L)
                .userId(recipient.getId())
                .content("我的原评论")
                .status(CommentStatus.APPROVED)
                .build();
        commentMapper.insert(originalComment);
        Comment replyComment = Comment.builder()
                .articleId(40001L)
                .userId(author.getId())
                .parentId(originalComment.getId())
                .rootId(originalComment.getId())
                .content("评论回复")
                .status(CommentStatus.APPROVED)
                .build();
        commentMapper.insert(replyComment);

        Message originalMessage = Message.builder()
                .userId(recipient.getId())
                .nickname(recipient.getNickname())
                .email("")
                .content("我的原留言")
                .status(MessageStatus.APPROVED)
                .notifyOnReply(false)
                .build();
        messageMapper.insert(originalMessage);
        Message replyMessage = Message.builder()
                .userId(author.getId())
                .parentId(originalMessage.getId())
                .nickname(author.getNickname())
                .email("")
                .content("留言回复")
                .status(MessageStatus.APPROVED)
                .notifyOnReply(false)
                .build();
        messageMapper.insert(replyMessage);

        for (var source : List.of(
                Notification.builder().type(NotificationType.COMMENT_REPLY)
                        .sourceType("COMMENT").sourceId(replyComment.getId())
                        .title("评论回复").content("评论回复").build(),
                Notification.builder().type(NotificationType.MESSAGE_REPLY)
                        .sourceType("MESSAGE").sourceId(replyMessage.getId())
                        .title("留言回复").content("留言回复").build())) {
            source.setTargetScope(NotificationTargetScope.SELECTED_USERS);
            source.setStatus(NotificationStatus.PUBLISHED);
            notificationMapper.insertNotification(source);
            notificationMapper.insertRecipientsForUsers(source.getId(), recipient.getId(), null);
        }

        NotificationQueryDTO query = new NotificationQueryDTO();
        var records = notificationService.getUserNotifications(recipient.getId(), query).getRecords();
        var commentNotification = records.stream().filter(item -> item.getType() == NotificationType.COMMENT_REPLY)
                .findFirst().orElseThrow();
        assertEquals(author.getNickname(), commentNotification.getAuthorName());
        assertEquals("我的原评论", commentNotification.getOriginalContent());
        assertEquals(40001L, commentNotification.getArticleId());
        assertEquals(originalComment.getId(), commentNotification.getParentId());
        var messageNotification = records.stream().filter(item -> item.getType() == NotificationType.MESSAGE_REPLY)
                .findFirst().orElseThrow();
        assertEquals(author.getNickname(), messageNotification.getAuthorName());
        assertEquals("我的原留言", messageNotification.getOriginalContent());
        assertEquals(originalMessage.getId(), messageNotification.getParentId());
        assertEquals(null, messageNotification.getLiked());
        assertEquals(null, messageNotification.getCanInteract());
        assertEquals(null, messageNotification.getLikeCount());
    }

    @Test
    void newAnnouncementShouldUnpinPreviousAnnouncement() {
        User admin = createUser("announcement_admin", UserRole.ADMIN);

        Long firstId = messageService.createAnnouncement(admin.getId(),
                CreateMessageRequestDTO.builder().content("第一条公告").build()).getId();
        Message ordinary = Message.builder()
                .userId(admin.getId())
                .nickname(admin.getNickname())
                .email("")
                .content("较新的普通留言")
                .isAnnouncement(false)
                .isPinned(false)
                .status(MessageStatus.APPROVED)
                .notifyOnReply(false)
                .build();
        messageMapper.insert(ordinary);
        Long secondId = messageService.createAnnouncement(admin.getId(),
                CreateMessageRequestDTO.builder().content("第二条公告").build()).getId();

        Message first = messageMapper.selectById(firstId);
        Message second = messageMapper.selectById(secondId);
        assertFalse(first.getIsPinned());
        assertTrue(second.getIsPinned());
        MessagePageQueryDTO query = new MessagePageQueryDTO();
        query.setPageSize(100);
        assertEquals(List.of(secondId, ordinary.getId(), firstId), messageService.getPublicMessageList(null, query).getRecords()
                .stream().filter(item -> List.of(firstId, ordinary.getId(), secondId).contains(item.getId()))
                .map(item -> item.getId()).toList());
    }

    private User createUser(String username, UserRole role) {
        User user = User.builder()
                .username(username)
                .nickname(username)
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

    private List<Long> recipientUserIds(Long notificationId) {
        return jdbcTemplate.queryForList(
                "SELECT user_id FROM blog_notification_recipient WHERE notification_id = ? ORDER BY user_id",
                Long.class,
                notificationId
        );
    }
}
