package com.ccsanjuu.blog.modules.message.service.impl;

import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.modules.message.service.MessageReplyNotificationService;
import com.ccsanjuu.blog.properties.BlogMailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MessageReplyNotificationServiceImpl implements MessageReplyNotificationService {

    private static final long[] RETRY_DELAYS_SECONDS = {2, 10, 30};

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final BlogMailProperties mailProperties;
    private final TaskExecutor messageMailTaskExecutor;

    public MessageReplyNotificationServiceImpl(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            BlogMailProperties mailProperties,
            TaskExecutor messageMailTaskExecutor
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
        this.messageMailTaskExecutor = messageMailTaskExecutor;
    }

    @Override
    public void sendAfterCommit(Message rootMessage, Message replyMessage) {
        if (!mailProperties.isEnabled()
                || !Boolean.TRUE.equals(rootMessage.getNotifyOnReply())
                || rootMessage.getEmail() == null || rootMessage.getEmail().isBlank()
                || rootMessage.getUnsubscribeToken() == null || rootMessage.getUnsubscribeToken().isBlank()) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submitMailTask(rootMessage, replyMessage);
                }
            });
        } else {
            submitMailTask(rootMessage, replyMessage);
        }
    }

    /**
     * 组装纯文本通知内容，避免在邮件中执行 HTML 或 Markdown。
     *
     * @param rootMessage 顶层留言
     * @param replyMessage 回复
     * @return 邮件正文
     */
    private String buildMailText(Message rootMessage, Message replyMessage) {
        String unsubscribeUrl = mailProperties.getFrontendBaseUrl()
                + "/messages/unsubscribe?token="
                + URLEncoder.encode(rootMessage.getUnsubscribeToken(), StandardCharsets.UTF_8);
        String excerpt = rootMessage.getContent().length() <= 120
                ? rootMessage.getContent()
                : rootMessage.getContent().substring(0, 120) + "...";
        return "你在 Sanjuu Blog 的留言收到了新的管理员回复。\n\n"
                + "你的留言：\n" + excerpt + "\n\n"
                + "管理员回复：\n"
                + replyMessage.getContent() + "\n\n"
                + "查看留言：" + mailProperties.getFrontendBaseUrl() + "/messages\n"
                + "关闭这条留言的后续通知：" + unsubscribeUrl;
    }

    /**
     * 发送通知，并对暂时性的 SMTP 错误做有限重试。
     *
     * @param rootMessage 顶层留言
     * @param replyMessage 管理员回复
     */
    private void sendWithRetry(Message rootMessage, Message replyMessage) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || !mailProperties.isEnabled()) {
            return;
        }

        for (int attempt = 1; attempt <= RETRY_DELAYS_SECONDS.length + 1; attempt++) {
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setFrom(mailProperties.getFrom());
                mail.setTo(rootMessage.getEmail());
                mail.setSubject("您在 Sanjuu Blog 留言有了新的回复");
                mail.setText(buildMailText(rootMessage, replyMessage));
                mailSender.send(mail);
                return;
            } catch (MailAuthenticationException | MailParseException | MailPreparationException exception) {
                log.error("留言回复通知邮件发送失败且不重试: messageId={}, attempts={}, exceptionType={}",
                        rootMessage.getId(), attempt, exception.getClass().getSimpleName());
                return;
            } catch (MailException exception) {
                if (attempt > RETRY_DELAYS_SECONDS.length) {
                    log.error("留言回复通知邮件最终发送失败: messageId={}, attempts={}, exceptionType={}",
                            rootMessage.getId(), attempt, exception.getClass().getSimpleName());
                    return;
                }
                try {
                    TimeUnit.SECONDS.sleep(RETRY_DELAYS_SECONDS[attempt - 1]);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    log.warn("留言回复通知邮件重试被中断: messageId={}", rootMessage.getId());
                    return;
                }
            } catch (RuntimeException exception) {
                log.error("留言回复通知邮件发送失败且不重试: messageId={}, attempts={}, exceptionType={}",
                        rootMessage.getId(), attempt, exception.getClass().getSimpleName());
                return;
            }
        }
    }

    /**
     * 把邮件发送任务提交到独立线程池。
     *
     * @param rootMessage 顶层留言
     * @param replyMessage 管理员回复
     */
    private void submitMailTask(Message rootMessage, Message replyMessage) {
        try {
            messageMailTaskExecutor.execute(() -> sendWithRetry(rootMessage, replyMessage));
        } catch (RuntimeException exception) {
            log.error("留言回复通知任务提交失败: messageId={}, exceptionType={}",
                    rootMessage.getId(), exception.getClass().getSimpleName());
        }
    }
}
