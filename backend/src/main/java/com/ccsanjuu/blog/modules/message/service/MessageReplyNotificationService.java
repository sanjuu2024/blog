package com.ccsanjuu.blog.modules.message.service;

import com.ccsanjuu.blog.modules.message.model.entity.Message;

public interface MessageReplyNotificationService {

    void sendAfterCommit(Message rootMessage, Message replyMessage);
}
