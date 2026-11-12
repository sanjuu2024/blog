package com.ccsanjuu.blog.modules.message.service.impl;

import com.ccsanjuu.blog.modules.comment.service.CommentStickerService;
import com.ccsanjuu.blog.modules.mail.model.bo.ReplyMailContent;
import com.ccsanjuu.blog.modules.mail.model.bo.MailMessageContent;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import com.ccsanjuu.blog.modules.mail.service.MailDeliveryService;
import com.ccsanjuu.blog.modules.mail.service.MailNotificationSender;
import com.ccsanjuu.blog.modules.mail.service.ReplyMailContentFactory;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.modules.message.model.enums.MessageStatus;
import com.ccsanjuu.blog.modules.message.service.MessageReplyNotificationService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
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
    private final UserMapper userMapper;
    private final CommentStickerService commentStickerService;

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
                        "MESSAGE_REPLY", rootMessage.getId(), replyMessage.getId(), rootMessage.getEmail(), rootMessage.getUserId());
                mailNotificationSender.submit(delivery,
                        () -> buildMailContent(rootMessage.getId(), replyMessage.getId()));
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
        mailNotificationSender.submit(delivery, () -> buildMailContent(delivery.getSourceId(), delivery.getReplyId()));
    }

    private MailMessageContent buildMailContent(
            Long sourceId,
            Long replyId
    ) {
        Message root = messageMapper.selectById(sourceId);
        Message reply = messageMapper.selectById(replyId);
        if (root == null || reply == null || root.getParentId() != null
                || !root.getId().equals(reply.getParentId())
                || root.getStatus() != MessageStatus.APPROVED || reply.getStatus() != MessageStatus.APPROVED) {
            throw new IllegalStateException("关联留言不可用");
        }
        if (!Boolean.TRUE.equals(root.getNotifyOnReply())
                || root.getEmail() == null || root.getEmail().isBlank()
                || root.getUnsubscribeToken() == null || root.getUnsubscribeToken().isBlank()) {
            throw new IllegalStateException("关联留言或收件地址不可用");
        }
        if (root.getUserId() != null) {
            var recipient = userMapper.selectById(root.getUserId());
            if (recipient == null || recipient.getDeletedAt() != null || !Boolean.TRUE.equals(recipient.getEmailVerified())) {
                throw new IllegalStateException("留言收件账号不可用");
            }
        }
        String unsubscribeUrl = mailProperties.getFrontendBaseUrl()
                + "/messages/unsubscribe?token="
                + URLEncoder.encode(root.getUnsubscribeToken(), StandardCharsets.UTF_8);
        return ReplyMailContentFactory.create(blogProperties.getAppName(), new ReplyMailContent(
                root.getEmail(),
                root.getNickname(),
                mailProperties.getFrontendBaseUrl(),
                "你在留言板的评论有了新的回复",
                "您曾在留言板上发表评论：",
                null,
                null,
                "",
                commentStickerService.toPlainText(root.getContent()),
                "管理员给您的回复如下：",
                commentStickerService.toPlainText(reply.getContent()),
                "查看回复完整内容",
                mailProperties.getFrontendBaseUrl() + "/messages",
                unsubscribeUrl
        ));
    }
}
