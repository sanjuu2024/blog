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

    /**
     * 获取公开留言及其可见回复。
     *
     * @param currentUserId 当前登录用户 ID，游客为 {@code null}
     * @param queryDTO 查询条件和分页参数
     * @return 公开留言分页结果
     */
    PageResult<PublicMessageItemVO> getPublicMessageList(Long currentUserId, MessagePageQueryDTO queryDTO);

    /**
     * 创建顶层留言。
     *
     * @param currentUserId 当前登录用户 ID，游客为 {@code null}
     * @param clientIp 客户端 IP
     * @param requestDTO 留言参数
     * @return 留言结果
     */
    MessageMutationVO createMessage(Long currentUserId, String clientIp, CreateMessageRequestDTO requestDTO);

    /**
     * 删除登录用户自己的顶层留言及其后代回复。
     *
     * @param messageId 留言 ID
     * @param userId 当前用户 ID
     */
    void deleteOwnMessage(Long messageId, Long userId);

    /**
     * 获取后台留言分页列表。
     *
     * @param queryDTO 查询条件和分页参数
     * @return 后台留言分页结果
     */
    PageResult<AdminMessageItemVO> getAdminMessageList(MessagePageQueryDTO queryDTO);

    /**
     * 审核、隐藏或删除留言。
     *
     * @param messageId 留言 ID
     * @param adminId 管理员 ID
     * @param requestDTO 审核操作参数
     * @return 处理后的留言
     */
    MessageMutationVO moderateMessage(Long messageId, Long adminId, MessageModerationRequestDTO requestDTO);

    /**
     * 管理员回复已通过的顶层留言。
     *
     * @param messageId 顶层留言 ID
     * @param adminId 管理员 ID
     * @param requestDTO 回复参数
     * @return 回复结果
     */
    MessageMutationVO replyMessage(Long messageId, Long adminId, CreateMessageReplyRequestDTO requestDTO);

    /**
     * 批量通过待审核顶层留言。
     *
     * @param adminId 管理员 ID
     * @param requestDTO 待审核留言 ID 列表
     */
    void approveMessages(Long adminId, MessageBatchApprovalRequestDTO requestDTO);

    /**
     * 幂等关闭单条顶层留言的后续回复通知。
     *
     * @param requestDTO 退订令牌
     */
    void unsubscribe(MessageUnsubscribeRequestDTO requestDTO);

    /**
     * 创建并置顶管理员公告留言。
     *
     * @param adminId 管理员 ID
     * @param requestDTO 公告参数
     * @return 公告留言结果
     */
    MessageMutationVO createAnnouncement(Long adminId, CreateMessageRequestDTO requestDTO);
}
