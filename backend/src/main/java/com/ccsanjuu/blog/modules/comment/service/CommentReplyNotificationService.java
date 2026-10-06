package com.ccsanjuu.blog.modules.comment.service;

import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;

public interface CommentReplyNotificationService {

    /**
     * 在业务事务提交后异步发送评论直接回复邮件。
     *
     * @param parentComment 被回复的评论
     * @param replyComment 直接回复
     */
    void sendAfterCommit(Comment parentComment, Comment replyComment);

    /**
     * 根据投递记录重新生成并发送评论回复邮件。
     *
     * @param delivery 邮件投递记录
     */
    void retry(MailDelivery delivery);
}
