package com.ccsanjuu.blog.modules.comment.service.impl;

import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
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
    private final CategoryMapper categoryMapper;

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
                        MailType.COMMENT_REPLY.name(), parentComment.getId(), replyComment.getId(), recipient.getEmail(), recipient.getId());
                mailNotificationSender.submit(delivery,
                        () -> buildMailContent(parentComment.getId(), replyComment.getId()));
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
        mailNotificationSender.submit(delivery, () -> buildMailContent(delivery.getSourceId(), delivery.getReplyId()));
    }

    private MailMessageContent buildMailContent(Long sourceId, Long replyId) {
        Comment parent = commentMapper.selectById(sourceId);
        Comment reply = commentMapper.selectById(replyId);
        if (parent == null || reply == null || !parent.getId().equals(reply.getParentId())
                || !parent.getArticleId().equals(reply.getArticleId())
                || parent.getStatus() != CommentStatus.APPROVED || reply.getStatus() != CommentStatus.APPROVED) {
            throw new IllegalStateException("评论回复关联数据不可用");
        }
        if (parent.getRootId() != null) {
            Comment root = commentMapper.selectById(parent.getRootId());
            if (root == null || root.getStatus() != CommentStatus.APPROVED) {
                throw new IllegalStateException("所属顶层评论不可见");
            }
        }
        User recipient = getSubscribedRecipient(parent);
        if (recipient == null) throw new IllegalStateException("评论订阅或收件地址不可用");
        Article article = articleMapper.selectById(reply.getArticleId());
        User replyAuthor = userMapper.selectById(reply.getUserId());
        if (article == null || article.getStatus() != ArticleStatus.PUBLISHED || replyAuthor == null) {
            throw new IllegalStateException("评论回复关联数据不可用");
        }
        var category = categoryMapper.selectById(article.getCategoryId());
        var parentCategory = category == null ? null : categoryMapper.selectById(category.getParentId());
        if (category == null || parentCategory == null
                || category.getStatus() != CategoryStatus.ENABLED || parentCategory.getStatus() != CategoryStatus.ENABLED) {
            throw new IllegalStateException("评论所属文章不可见");
        }

        String unsubscribeUrl = mailProperties.getFrontendBaseUrl()
                + "/comments/unsubscribe?token="
                + URLEncoder.encode(parent.getUnsubscribeToken(), StandardCharsets.UTF_8);
        String articleUrl = mailProperties.getFrontendBaseUrl()
                + "/articles/" + article.getId()
                + "?replyId=" + reply.getId() + "#article-comments";
        return ReplyMailContentFactory.create(blogProperties.getAppName(), new ReplyMailContent(
                recipient.getEmail(),
                resolveRecipientName(recipient.getNickname(), recipient.getUsername()),
                mailProperties.getFrontendBaseUrl(),
                "你在[" + article.getTitle() + "]的评论有了新的回复",
                "您曾在文章《" + article.getTitle() + "》上发表评论：",
                null,
                null,
                "",
                parent.getContent(),
                replyAuthor.getNickname() + " 给您的回复如下：",
                reply.getContent(),
                "查看回复完整内容",
                articleUrl,
                unsubscribeUrl
        ));
    }

    private String resolveRecipientName(String nickname, String username) {
        return nickname == null || nickname.isBlank() ? username : nickname;
    }

    private User getSubscribedRecipient(Comment comment) {
        if (!mailProperties.isEnabled()
                || !Boolean.TRUE.equals(comment.getNotifyOnReply())
                || comment.getUnsubscribeToken() == null || comment.getUnsubscribeToken().isBlank()) {
            return null;
        }
        User user = userMapper.selectById(comment.getUserId());
        return user != null
                && user.getDeletedAt() == null
                && Boolean.TRUE.equals(user.getEmailVerified())
                && user.getEmail() != null && !user.getEmail().isBlank()
                ? user : null;
    }
}
