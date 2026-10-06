package com.ccsanjuu.blog.modules.message.service.impl;

import com.ccsanjuu.blog.modules.mail.model.bo.ReplyMailContent;
import com.ccsanjuu.blog.modules.mail.model.bo.MailMessageContent;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import com.ccsanjuu.blog.modules.mail.service.MailDeliveryService;
import com.ccsanjuu.blog.modules.mail.service.MailNotificationSender;
import com.ccsanjuu.blog.modules.mail.service.ReplyMailContentFactory;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.modules.message.service.MessageReplyNotificationService;
import com.ccsanjuu.blog.properties.BlogProperties;
import com.ccsanjuu.blog.properties.BlogMailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 处理留言回复邮件的订阅判断、正文生成和失败重试。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageReplyNotificationServiceImpl implements MessageReplyNotificationService {

    private final BlogMailProperties mailProperties;
    private final BlogProperties blogProperties;
    private final MailDeliveryService mailDeliveryService;
    private final MailNotificationSender mailNotificationSender;
    private final MessageMapper messageMapper;

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

        Runnable task = () -> {
            try {
                MailDelivery delivery = mailDeliveryService.createPending(
                        "MESSAGE_REPLY", rootMessage.getId(), replyMessage.getId(), rootMessage.getEmail());
                mailNotificationSender.submit(delivery,
                        () -> buildMailContent(rootMessage, replyMessage));
            } catch (RuntimeException exception) {
                log.error("留言回复邮件调度失败: messageId={}, replyId={}, exceptionType={}",
                        rootMessage.getId(), replyMessage.getId(), exception.getClass().getSimpleName());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    /**
     * 根据投递记录关联的留言和回复重新生成并发送邮件。
     *
     * @param delivery 投递记录
     */
    @Override
    public void retry(MailDelivery delivery) {
        mailNotificationSender.submit(delivery, () -> {
            Message root = messageMapper.selectById(delivery.getSourceId());
            Message reply = messageMapper.selectById(delivery.getReplyId());
            if (root == null || reply == null) throw new IllegalStateException("关联留言不可用");
            return buildMailContent(root, reply);
        });
    }

    private MailMessageContent buildMailContent(
            Message root,
            Message reply
    ) {
        if (root.getEmail() == null || root.getEmail().isBlank()
                || root.getUnsubscribeToken() == null || root.getUnsubscribeToken().isBlank()) {
            throw new IllegalStateException("关联留言或收件地址不可用");
        }
        String unsubscribeUrl = mailProperties.getFrontendBaseUrl()
                + "/messages/unsubscribe?token="
                + URLEncoder.encode(root.getUnsubscribeToken(), StandardCharsets.UTF_8);
        return ReplyMailContentFactory.create(blogProperties.getAppName(), new ReplyMailContent(
                root.getEmail(),
                "您在 " + blogProperties.getAppName() + " 留言有了新的回复",
                "你的留言收到了新的管理员回复。",
                null,
                null,
                "你的留言",
                root.getContent(),
                "管理员回复",
                reply.getContent(),
                "查看留言",
                mailProperties.getFrontendBaseUrl() + "/messages",
                unsubscribeUrl
        ));
    }
}
