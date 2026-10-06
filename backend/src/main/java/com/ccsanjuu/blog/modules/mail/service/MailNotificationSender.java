package com.ccsanjuu.blog.modules.mail.service;

import com.ccsanjuu.blog.modules.mail.model.bo.MailMessageContent;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import com.ccsanjuu.blog.modules.mail.model.enums.MailDeliveryStatus;
import com.ccsanjuu.blog.properties.BlogMailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 统一处理回复通知邮件的异步发送、multipart 正文和有限重试。
 */
@Slf4j
@Service
public class MailNotificationSender {

    private static final long[] RETRY_DELAYS_SECONDS = {2, 10, 30};

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final BlogMailProperties mailProperties;
    private final TaskExecutor taskExecutor;
    private final MailDeliveryService mailDeliveryService;

    /**
     * 创建统一邮件发送器。
     *
     * @param mailSenderProvider 邮件发送器提供者
     * @param mailProperties 邮件配置
     * @param taskExecutor 邮件任务线程池
     * @param mailDeliveryService 投递记录服务
     */
    public MailNotificationSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            BlogMailProperties mailProperties,
            @Qualifier("messageMailTaskExecutor") TaskExecutor taskExecutor,
            MailDeliveryService mailDeliveryService
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
        this.taskExecutor = taskExecutor;
        this.mailDeliveryService = mailDeliveryService;
    }

    /**
     * 将邮件投递任务提交到独立线程池。
     *
     * @param delivery 投递记录
     * @param contentSupplier 邮件内容生成器
     */
    public void submit(MailDelivery delivery, Supplier<MailMessageContent> contentSupplier) {
        try {
            taskExecutor.execute(() -> sendWithRetry(delivery, contentSupplier));
        } catch (RuntimeException exception) {
            mailDeliveryService.markAttempt(delivery.getId(), MailDeliveryStatus.FAILED,
                    exception.getClass().getSimpleName(), "邮件发送任务提交失败");
        }
    }

    private void sendWithRetry(MailDelivery delivery, Supplier<MailMessageContent> contentSupplier) {
        if (!mailProperties.isEnabled()) {
            mailDeliveryService.markAttempt(delivery.getId(), MailDeliveryStatus.FAILED,
                    "MAIL_DISABLED", "邮件功能未启用");
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            mailDeliveryService.markAttempt(delivery.getId(), MailDeliveryStatus.FAILED,
                    "MAIL_SENDER_UNAVAILABLE", "邮件发送器不可用");
            log.error("notification_event=REPLY_MAIL_FAILED outcome=FAIL reason=MAIL_SENDER_UNAVAILABLE deliveryId={}",
                    delivery.getId());
            return;
        }

        for (int attempt = 1; attempt <= RETRY_DELAYS_SECONDS.length + 1; attempt++) {
            try {
                MailMessageContent content = contentSupplier.get();
                var mimeMessage = mailSender.createMimeMessage();
                mimeMessage.setFrom(new InternetAddress(mailProperties.getFrom()));
                mimeMessage.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(content.recipient(), false));
                mimeMessage.setSubject(content.subject(), "UTF-8");
                Multipart alternative = new MimeMultipart("alternative");
                MimeBodyPart plainPart = new MimeBodyPart();
                plainPart.setText(content.plainText(), "UTF-8", "plain");
                alternative.addBodyPart(plainPart);
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setText(content.html(), "UTF-8", "html");
                alternative.addBodyPart(htmlPart);
                mimeMessage.setContent(alternative);
                mimeMessage.saveChanges();
                mailSender.send(mimeMessage);
                mailDeliveryService.markSent(delivery.getId());
                return;
            } catch (MailAuthenticationException | MailParseException | MailPreparationException exception) {
                mailDeliveryService.markAttempt(delivery.getId(), MailDeliveryStatus.FAILED,
                        exception.getClass().getSimpleName(), getFailureMessage(exception));
                return;
            } catch (MessagingException exception) {
                mailDeliveryService.markAttempt(delivery.getId(), MailDeliveryStatus.FAILED,
                        exception.getClass().getSimpleName(), "邮件内容组装失败");
                return;
            } catch (MailException exception) {
                mailDeliveryService.markAttempt(delivery.getId(),
                        attempt > RETRY_DELAYS_SECONDS.length
                                ? MailDeliveryStatus.FAILED : MailDeliveryStatus.PENDING,
                        exception.getClass().getSimpleName(), getFailureMessage(exception));
                if (attempt > RETRY_DELAYS_SECONDS.length) return;
                try {
                    TimeUnit.SECONDS.sleep(RETRY_DELAYS_SECONDS[attempt - 1]);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    mailDeliveryService.markAttempt(delivery.getId(), MailDeliveryStatus.FAILED,
                            "INTERRUPTED", "邮件重试被中断");
                    return;
                }
            } catch (RuntimeException exception) {
                mailDeliveryService.markAttempt(delivery.getId(), MailDeliveryStatus.FAILED,
                        exception.getClass().getSimpleName(), "邮件发送过程中发生运行时错误");
                return;
            }
        }
    }

    private String getFailureMessage(MailException exception) {
        if (exception instanceof MailAuthenticationException) return "SMTP 认证失败";
        if (exception instanceof MailParseException) return "邮件内容解析失败";
        if (exception instanceof MailPreparationException) return "邮件准备失败";
        return "SMTP 发送失败";
    }
}
