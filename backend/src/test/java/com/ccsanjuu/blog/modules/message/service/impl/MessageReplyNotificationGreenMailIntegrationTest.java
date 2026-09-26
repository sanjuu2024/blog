package com.ccsanjuu.blog.modules.message.service.impl;

import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.modules.message.service.MessageReplyNotificationService;
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

    @MockitoBean(name = "messageMailTaskExecutor")
    private TaskExecutor messageMailTaskExecutor;

    @BeforeEach
    void executeMailTasksSynchronously() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
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

        transactionTemplate.executeWithoutResult(status -> {
            notificationService.sendAfterCommit(rootMessage(), replyMessage());

            // 事务提交前只注册回调，不应提前把邮件交给 SMTP 服务。
            assertFalse(GREEN_MAIL.waitForIncomingEmail(200, 1));
        });

        assertTrue(GREEN_MAIL.waitForIncomingEmail(5000, 1));
        MimeMessage received = GREEN_MAIL.getReceivedMessages()[0];
        String body = received.getContent().toString();

        assertEquals("notifications@sanjuu.test", received.getFrom()[0].toString());
        assertEquals("guest@example.test", received.getAllRecipients()[0].toString());
        assertEquals("您在 Sanjuu Blog 留言有了新的回复", received.getSubject());
        assertTrue(body.contains("这是一条测试留言"));
        assertTrue(body.contains("这是管理员的测试回复"));
        assertTrue(body.contains("http://localhost:5173/messages"));
        assertTrue(body.contains(
                "http://localhost:5173/messages/unsubscribe?token=unsubscribe-token"
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

    private Message rootMessage() {
        return Message.builder()
                .id(90001L)
                .email("guest@example.test")
                .content("这是一条测试留言")
                .notifyOnReply(true)
                .unsubscribeToken("unsubscribe-token")
                .build();
    }

    private Message replyMessage() {
        return Message.builder()
                .id(90002L)
                .parentId(90001L)
                .content("这是管理员的测试回复")
                .build();
    }
}
