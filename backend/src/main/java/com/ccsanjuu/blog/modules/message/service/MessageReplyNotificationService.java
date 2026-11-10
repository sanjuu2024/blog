package com.ccsanjuu.blog.modules.message.service;

import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;

public interface MessageReplyNotificationService {

    /**
     * 在业务事务提交后异步发送留言回复邮件。
     *
     * @param rootMessage 顶层留言
     * @param replyMessage 管理员回复
     */
    void sendAfterCommit(Message rootMessage, Message replyMessage);

    /**
     * 根据投递记录关联的留言和回复重新生成并发送邮件。
     *
     * @param delivery 投递记录
     */
    void retry(MailDelivery delivery);
}
