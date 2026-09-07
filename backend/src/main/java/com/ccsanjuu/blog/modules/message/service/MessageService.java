package com.ccsanjuu.blog.modules.message.service;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.message.model.dto.CreateMessageReplyRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.CreateMessageRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessageBatchApprovalRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessageModerationRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessagePageQueryDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessageUnsubscribeRequestDTO;
import com.ccsanjuu.blog.modules.message.model.vo.AdminMessageItemVO;
import com.ccsanjuu.blog.modules.message.model.vo.MessageMutationVO;
import com.ccsanjuu.blog.modules.message.model.vo.PublicMessageItemVO;

public interface MessageService {

    PageResult<PublicMessageItemVO> getPublicMessageList(Long currentUserId, MessagePageQueryDTO queryDTO);

    MessageMutationVO createMessage(Long currentUserId, String clientIp, CreateMessageRequestDTO requestDTO);

    void deleteOwnMessage(Long messageId, Long userId);

    PageResult<AdminMessageItemVO> getAdminMessageList(MessagePageQueryDTO queryDTO);

    MessageMutationVO moderateMessage(Long messageId, Long adminId, MessageModerationRequestDTO requestDTO);

    MessageMutationVO replyMessage(Long messageId, Long adminId, CreateMessageReplyRequestDTO requestDTO);

    void approveMessages(Long adminId, MessageBatchApprovalRequestDTO requestDTO);

    void unsubscribe(MessageUnsubscribeRequestDTO requestDTO);
}
