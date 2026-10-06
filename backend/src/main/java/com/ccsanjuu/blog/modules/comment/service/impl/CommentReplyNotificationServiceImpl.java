package com.ccsanjuu.blog.modules.comment.service.impl;

import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import com.ccsanjuu.blog.modules.comment.service.CommentReplyNotificationService;
import com.ccsanjuu.blog.modules.mail.model.bo.MailMessageContent;
import com.ccsanjuu.blog.modules.mail.model.bo.ReplyMailContent;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import com.ccsanjuu.blog.modules.mail.model.enums.MailType;
import com.ccsanjuu.blog.modules.mail.service.MailDeliveryService;
import com.ccsanjuu.blog.modules.mail.service.MailNotificationSender;
import com.ccsanjuu.blog.modules.mail.service.ReplyMailContentFactory;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
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
 * 处理评论直接回复邮件的订阅判断、正文生成和失败重试。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentReplyNotificationServiceImpl implements CommentReplyNotificationService {

    private final BlogMailProperties mailProperties;
    private final BlogProperties blogProperties;
    private final MailDeliveryService mailDeliveryService;
    private final MailNotificationSender mailNotificationSender;
    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;

    /**
     * 在事务提交后创建评论回复邮件投递记录，避免回滚事务发送邮件。
     *
     * @param parentComment 被回复的评论
     * @param replyComment 直接回复
     */
    @Override
    public void sendAfterCommit(Comment parentComment, Comment replyComment) {
        User recipient = getSubscribedRecipient(parentComment);
        if (recipient == null || parentComment.getUserId().equals(replyComment.getUserId())) return;

        Runnable task = () -> {
            try {
                MailDelivery delivery = mailDeliveryService.createPending(
                        MailType.COMMENT_REPLY.name(), parentComment.getId(), replyComment.getId(), recipient.getEmail());
                mailNotificationSender.submit(delivery,
                        () -> buildMailContent(parentComment, replyComment, recipient));
            } catch (RuntimeException exception) {
                log.error("评论回复邮件调度失败: commentId={}, replyId={}, exceptionType={}",
                        parentComment.getId(), replyComment.getId(), exception.getClass().getSimpleName());
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
     * 根据投递记录重新查询评论业务数据并重试发送。
     *
     * @param delivery 邮件投递记录
     */
    @Override
    public void retry(MailDelivery delivery) {
        mailNotificationSender.submit(delivery, () -> {
            Comment parent = commentMapper.selectById(delivery.getSourceId());
            Comment reply = commentMapper.selectById(delivery.getReplyId());
            if (parent == null || reply == null) throw new IllegalStateException("评论回复关联数据不可用");
            User recipient = getSubscribedRecipient(parent);
            if (recipient == null) throw new IllegalStateException("评论订阅或收件地址不可用");
            return buildMailContent(parent, reply, recipient);
        });
    }

    private MailMessageContent buildMailContent(Comment parent, Comment reply, User recipient) {
        Article article = articleMapper.selectById(reply.getArticleId());
        User replyAuthor = userMapper.selectById(reply.getUserId());
        if (article == null || replyAuthor == null) throw new IllegalStateException("评论回复关联数据不可用");

        String unsubscribeUrl = mailProperties.getFrontendBaseUrl()
                + "/comments/unsubscribe?token="
                + URLEncoder.encode(parent.getUnsubscribeToken(), StandardCharsets.UTF_8);
        String articleUrl = mailProperties.getFrontendBaseUrl()
                + "/articles/" + article.getId()
                + "?replyId=" + reply.getId() + "#article-comments";
        return ReplyMailContentFactory.create(blogProperties.getAppName(), new ReplyMailContent(
                recipient.getEmail(),
                blogProperties.getAppName() + " 评论收到新的回复",
                "你的评论收到了新的直接回复。",
                "文章",
                article.getTitle(),
                "你的评论",
                parent.getContent(),
                replyAuthor.getNickname() + " 的回复",
                reply.getContent(),
                "查看回复",
                articleUrl,
                unsubscribeUrl
        ));
    }

    private User getSubscribedRecipient(Comment comment) {
        if (!mailProperties.isEnabled()
                || !Boolean.TRUE.equals(comment.getNotifyOnReply())
                || comment.getUnsubscribeToken() == null || comment.getUnsubscribeToken().isBlank()) {
            return null;
        }
        User user = userMapper.selectById(comment.getUserId());
        return user != null
                && Boolean.TRUE.equals(user.getEmailVerified())
                && user.getEmail() != null && !user.getEmail().isBlank()
                ? user : null;
    }
}
