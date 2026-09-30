package com.ccsanjuu.blog.modules.message.service.impl;

import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.message.service.MessageReplyNotificationService;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import com.ccsanjuu.blog.modules.mail.model.enums.MailDeliveryStatus;
import com.ccsanjuu.blog.modules.mail.service.MailDeliveryService;
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
    private final MailDeliveryService mailDeliveryService;
    private final MessageMapper messageMapper;

    public MessageReplyNotificationServiceImpl(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            BlogMailProperties mailProperties,
            TaskExecutor messageMailTaskExecutor,
            MailDeliveryService mailDeliveryService,
            MessageMapper messageMapper
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
        this.messageMailTaskExecutor = messageMailTaskExecutor;
        this.mailDeliveryService = mailDeliveryService;
        this.messageMapper = messageMapper;
    }

    /**
     * 在业务事务提交后异步发送留言回复邮件。
     *
     * @param rootMessage 顶层留言
     * @param replyMessage 管理员回复
     */
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
     * 根据投递记录关联的留言和回复重新生成并发送邮件。
     *
     * @param delivery 投递记录
     */
    @Override
    public void retry(MailDelivery delivery) {
        Message root = messageMapper.selectById(delivery.getSourceId());
        Message reply = messageMapper.selectById(delivery.getReplyId());
        if (root == null || reply == null || root.getEmail() == null || root.getEmail().isBlank()) {
            mailDeliveryService.markAttempt(delivery.getId(), MailDeliveryStatus.FAILED,
                    "SOURCE_UNAVAILABLE", "关联留言或收件地址不可用");
            return;
        }
        submitMailTask(root, reply, delivery.getId());
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
    private void sendWithRetry(Message rootMessage, Message replyMessage, Long deliveryId) {
        if (!mailProperties.isEnabled()) {
            mailDeliveryService.markAttempt(deliveryId, MailDeliveryStatus.FAILED,
                    "MAIL_DISABLED", "邮件功能未启用");
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            mailDeliveryService.markAttempt(deliveryId, MailDeliveryStatus.FAILED, "MAIL_SENDER_UNAVAILABLE", "邮件发送器不可用");
            log.error(
                    "notification_event=MESSAGE_REPLY_MAIL_FAILED description=\"留言回复通知邮件发送失败：邮件发送器不可用\" outcome=FAIL reason=MAIL_SENDER_UNAVAILABLE messageId={} replyId={}",
                    rootMessage.getId(),
                    replyMessage.getId()
            );
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
                mailDeliveryService.markSent(deliveryId);
                return;
            } catch (MailAuthenticationException | MailParseException | MailPreparationException exception) {
                log.error("留言回复通知邮件发送失败且不重试: messageId={}, attempts={}, exceptionType={}",
                        rootMessage.getId(), attempt, exception.getClass().getSimpleName());
                mailDeliveryService.markAttempt(deliveryId, MailDeliveryStatus.FAILED,
                        exception.getClass().getSimpleName(), getFailureMessage(exception));
                return;
            } catch (MailException exception) {
                mailDeliveryService.markAttempt(deliveryId,
                        attempt > RETRY_DELAYS_SECONDS.length
                                ? MailDeliveryStatus.FAILED : MailDeliveryStatus.PENDING,
                        exception.getClass().getSimpleName(), getFailureMessage(exception));
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
                    mailDeliveryService.markAttempt(deliveryId, MailDeliveryStatus.FAILED,
                            "INTERRUPTED", "邮件重试被中断");
                    return;
                }
            } catch (RuntimeException exception) {
                log.error("留言回复通知邮件发送失败且不重试: messageId={}, attempts={}, exceptionType={}",
                        rootMessage.getId(), attempt, exception.getClass().getSimpleName());
                mailDeliveryService.markAttempt(deliveryId, MailDeliveryStatus.FAILED,
                        exception.getClass().getSimpleName(), "邮件发送过程中发生运行时错误");
                return;
            }
        }
    }

    /**
     * 将邮件异常转换为不暴露敏感信息的失败摘要。
     *
     * @param exception 邮件异常
     * @return 安全失败摘要
     */
    private String getFailureMessage(MailException exception) {
        if (exception instanceof MailAuthenticationException) {
            return "SMTP 认证失败";
        }
        if (exception instanceof MailParseException) {
            return "邮件内容解析失败";
        }
        if (exception instanceof MailPreparationException) {
            return "邮件准备失败";
        }
        return "SMTP 发送失败";
    }

    /**
     * 把邮件发送任务提交到独立线程池。
     *
     * @param rootMessage 顶层留言
     * @param replyMessage 管理员回复
     */
    private void submitMailTask(Message rootMessage, Message replyMessage) {
        MailDelivery delivery = mailDeliveryService.createPending(
                "MESSAGE_REPLY", rootMessage.getId(), replyMessage.getId(), rootMessage.getEmail());
        submitMailTask(rootMessage, replyMessage, delivery.getId());
    }

    private void submitMailTask(Message rootMessage, Message replyMessage, Long deliveryId) {
        try {
            messageMailTaskExecutor.execute(() -> sendWithRetry(rootMessage, replyMessage, deliveryId));
        } catch (RuntimeException exception) {
            log.error("留言回复通知任务提交失败: messageId={}, exceptionType={}",
                    rootMessage.getId(), exception.getClass().getSimpleName());
            mailDeliveryService.markAttempt(deliveryId, MailDeliveryStatus.FAILED,
                    exception.getClass().getSimpleName(), "邮件发送任务提交失败");
        }
    }
}
