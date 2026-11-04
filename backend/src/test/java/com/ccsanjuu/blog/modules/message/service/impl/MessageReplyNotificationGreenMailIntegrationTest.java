package com.ccsanjuu.blog.modules.message.service.impl;

import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.message.model.enums.MessageStatus;
import com.ccsanjuu.blog.modules.message.service.MessageReplyNotificationService;
import com.ccsanjuu.blog.modules.mail.service.MailDeliveryService;
import com.ccsanjuu.blog.modules.mail.mapper.MailDeliveryMapper;
import com.ccsanjuu.blog.modules.mail.model.enums.MailDeliveryStatus;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "blog.mail.enabled=true",
        "blog.mail.from=notifications@sanjuu.test",
        "blog.mail.frontend-base-url=http://localhost:5173",
        "spring.mail.username=greenmail-user",
        "spring.mail.password=greenmail-password",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false"
})
class MessageReplyNotificationGreenMailIntegrationTest {

    private static final String SMTP_USERNAME = "greenmail-user";
    private static final String SMTP_PASSWORD = "greenmail-password";

    private static final GreenMail GREEN_MAIL = new GreenMail(
            new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP)
    );

    static {
        GREEN_MAIL.start();
        GREEN_MAIL.setUser("notifications@sanjuu.test", SMTP_USERNAME, SMTP_PASSWORD);
    }

    @Autowired
    private MessageReplyNotificationService notificationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private MailDeliveryService mailDeliveryService;

    @Autowired
    private MailDeliveryMapper mailDeliveryMapper;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<Long> messageIds = new ArrayList<>();

    @MockitoBean(name = "messageMailTaskExecutor")
    private TaskExecutor messageMailTaskExecutor;

    @BeforeEach
    void executeMailTasksSynchronously() {
        doAnswer(invocation -> {
            try (var executor = Executors.newSingleThreadExecutor()) {
                executor.submit(invocation.<Runnable>getArgument(0)).get(10, TimeUnit.SECONDS);
            }
            return null;
        }).when(messageMailTaskExecutor).execute(any(Runnable.class));
    }

    /**
     * 将 Spring Mail 指向当前测试进程中的 GreenMail 动态端口。
     *
     * @param registry Spring 测试属性注册器
     */
    @DynamicPropertySource
    static void registerMailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> GREEN_MAIL.getSmtp().getPort());
    }

    /**
     * 每个测试方法结束后清空邮箱
     *
     * @throws Exception
     */
    @AfterEach
    void clearMailbox() throws Exception {
        GREEN_MAIL.purgeEmailFromAllMailboxes();
        messageIds.forEach(id -> jdbc.update("DELETE FROM blog_mail_delivery WHERE mail_type = 'MESSAGE_REPLY' AND source_id = ?", id));
        messageIds.forEach(id -> jdbc.update("DELETE FROM blog_message_board WHERE id = ?", id));
    }

    /**
     * 所有测试结束后关闭 GreenMail 服务器
     *
     */
    @AfterAll
    static void stopGreenMail() {
        GREEN_MAIL.stop();
    }

    @Test
    void subscribedMessageShouldSendMailThroughSmtpAfterTransactionCommit() throws Exception {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Message root = rootMessage();
        Message reply = replyMessage();

        transactionTemplate.executeWithoutResult(status -> {
            persistMessages(root, reply);
            notificationService.sendAfterCommit(root, reply);

            // 事务提交前只注册回调，不应提前把邮件交给 SMTP 服务。
            assertFalse(GREEN_MAIL.waitForIncomingEmail(200, 1));
        });

        assertTrue(GREEN_MAIL.waitForIncomingEmail(5000, 1));
        Long deliveryId = jdbc.queryForObject("SELECT id FROM blog_mail_delivery WHERE mail_type = 'MESSAGE_REPLY' AND source_id = ?", Long.class, root.getId());
        var delivery = mailDeliveryService.getDetail(deliveryId);
        assertEquals(MailDeliveryStatus.SENT, delivery.getStatus());
        assertEquals(1, delivery.getAttemptCount());
        assertEquals("guest@example.test", delivery.getRecipientEmail());
        jdbc.update("UPDATE blog_mail_delivery SET recipient_email = NULL, recipient_masked = ? WHERE id = ?",
                "g***@example.test", deliveryId);
        var legacy = mailDeliveryService.getDetail(deliveryId);
        assertEquals("g***@example.test", legacy.getRecipientMasked());
        assertEquals("guest@example.test", legacy.getCurrentRecipientEmail());
        MimeMessage received = GREEN_MAIL.getReceivedMessages()[0];
        jakarta.mail.Multipart multipart = (jakarta.mail.Multipart) received.getContent();
        String plainText = multipart.getBodyPart(0).getContent().toString();
        String html = multipart.getBodyPart(1).getContent().toString();

        assertEquals("notifications@sanjuu.test", received.getFrom()[0].toString());
        assertEquals("guest@example.test", received.getAllRecipients()[0].toString());
        assertEquals("你在留言板的评论有了新的回复", received.getSubject());
        assertTrue(plainText.contains("这是一条测试留言"));
        assertTrue(plainText.contains("这是管理员的测试回复"));
        assertTrue(plainText.contains("http://localhost:5173/messages"));
        assertTrue(html.contains("这是一条测试留言"));
        assertTrue(html.contains("这是管理员的测试回复"));
        assertTrue(html.contains("您在 [集成测试站点] 的评论有了新的回复！"));
        assertTrue(html.contains("查看回复完整内容"));
        assertTrue(html.contains("href=\"http://localhost:5173\""));
        assertFalse(html.contains("display:inline-block"));
        assertTrue(html.contains(
                "http://localhost:5173/messages/unsubscribe?token=" + root.getUnsubscribeToken()
        ));
    }

    @Test
    void unsubscribedMessageShouldNotSendMail() {
        Message rootMessage = rootMessage();
        rootMessage.setNotifyOnReply(false);

        notificationService.sendAfterCommit(rootMessage, replyMessage());

        assertFalse(GREEN_MAIL.waitForIncomingEmail(300, 1));
        assertEquals(0, GREEN_MAIL.getReceivedMessages().length);
    }

    @Test
    void messageWithoutRecipientShouldNotSendMail() {
        Message rootMessage = rootMessage();
        rootMessage.setEmail(" ");

        notificationService.sendAfterCommit(rootMessage, replyMessage());

        assertFalse(GREEN_MAIL.waitForIncomingEmail(300, 1));
        assertEquals(0, GREEN_MAIL.getReceivedMessages().length);
    }

    @Test
    void rollbackShouldNotCreateDeliveryOrSendMail() {
        Message root = rootMessage();
        Message reply = replyMessage();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            persistMessages(root, reply);
            notificationService.sendAfterCommit(root, reply);
            status.setRollbackOnly();
        });

        assertEquals(0, GREEN_MAIL.getReceivedMessages().length);
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM blog_mail_delivery WHERE mail_type = 'MESSAGE_REPLY' AND source_id = ?", Long.class, root.getId()));
    }

    @Test
    void retryShouldPersistFailureWithoutSendingAfterUnsubscribe() {
        Message root = rootMessage();
        Message reply = replyMessage();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> persistMessages(root, reply));
        var delivery = mailDeliveryService.createPending("MESSAGE_REPLY", root.getId(), reply.getId(), root.getEmail(), null);
        mailDeliveryService.markAttempt(delivery.getId(), MailDeliveryStatus.FAILED, "SMTP", "测试失败");
        jdbc.update("UPDATE blog_message_board SET notify_on_reply = false WHERE id = ?", root.getId());

        mailDeliveryService.retry(delivery.getId());

        var current = mailDeliveryService.getDetail(delivery.getId());
        assertEquals(MailDeliveryStatus.FAILED, current.getStatus());
        assertEquals(2, current.getAttemptCount());
        assertEquals("guest@example.test", current.getRecipientEmail());
        assertEquals(0, GREEN_MAIL.getReceivedMessages().length);
    }

    @Test
    void rolledBackManualRetryShouldNotStartTask() {
        Message root = rootMessage();
        Message reply = replyMessage();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> persistMessages(root, reply));
        var delivery = mailDeliveryService.createPending("MESSAGE_REPLY", root.getId(), reply.getId(), root.getEmail(), null);
        mailDeliveryService.markAttempt(delivery.getId(), MailDeliveryStatus.FAILED, "SMTP", "测试失败");

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            mailDeliveryService.retry(delivery.getId());
            status.setRollbackOnly();
        });

        assertEquals(MailDeliveryStatus.FAILED, mailDeliveryMapper.selectById(delivery.getId()).getStatus());
        assertEquals(0, GREEN_MAIL.getReceivedMessages().length);
    }

    /**
     * 在真实业务事务中保存原留言和回复，供提交后的发送线程重新读取。
     *
     * @param root 原留言
     * @param reply 回复
     */
    private void persistMessages(Message root, Message reply) {
        root.setId(null);
        root.setUnsubscribeToken("greenmail-" + UUID.randomUUID());
        messageMapper.insert(root);
        messageIds.add(root.getId());
        reply.setId(null);
        reply.setParentId(root.getId());
        messageMapper.insert(reply);
        messageIds.add(reply.getId());
    }

    private Message rootMessage() {
        return Message.builder()
                .id(90001L)
                .email("guest@example.test")
                .nickname("测试访客")
                .status(MessageStatus.APPROVED)
                .content("这是一条测试留言")
                .notifyOnReply(true)
                .unsubscribeToken("unsubscribe-token")
                .build();
    }

    private Message replyMessage() {
        return Message.builder()
                .id(90002L)
                .parentId(90001L)
                .email("")
                .nickname("管理员")
                .notifyOnReply(false)
                .status(MessageStatus.APPROVED)
                .content("这是管理员的测试回复")
                .build();
    }
}
