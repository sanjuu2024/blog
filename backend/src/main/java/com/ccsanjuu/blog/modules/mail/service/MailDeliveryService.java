package com.ccsanjuu.blog.modules.mail.service;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.mail.model.dto.MailDeliveryQueryDTO;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import com.ccsanjuu.blog.modules.mail.model.enums.MailDeliveryStatus;
import com.ccsanjuu.blog.modules.mail.model.vo.MailDeliveryItemVO;

public interface MailDeliveryService {
    /**
     * 查询邮件投递分页记录。
     *
     * @param queryDTO 查询条件和分页参数
     * @return 邮件投递分页结果
     */
    PageResult<MailDeliveryItemVO> getPage(MailDeliveryQueryDTO queryDTO);

    /**
     * 查询邮件投递详情。
     *
     * @param deliveryId 投递记录 ID
     * @return 邮件投递详情
     */
    MailDeliveryItemVO getDetail(Long deliveryId);

    /**
     * 重试一条失败的邮件投递。
     *
     * @param deliveryId 投递记录 ID
     */
    void retry(Long deliveryId);

    /**
     * 创建待发送投递记录。
     *
     * @param mailType 邮件类型
     * @param sourceId 原始业务 ID
     * @param replyId 回复 ID
     * @param recipient 完整收件地址
     * @param userId 收件用户 ID，游客为空
     * @return 待发送投递记录
     */
    MailDelivery createPending(String mailType, Long sourceId, Long replyId, String recipient, Long userId);

    /**
     * 在实际发送前保存本次收件目标，避免重试后仍展示旧地址。
     *
     * @param deliveryId 投递记录 ID
     * @param recipient 完整收件地址
     */
    void updateRecipient(Long deliveryId, String recipient);

    /**
     * 更新一次失败尝试及其安全错误摘要。
     *
     * @param deliveryId 投递记录 ID
     * @param status 投递状态
     * @param errorType 安全错误类型
     * @param errorMessage 安全错误摘要
     */
    void markAttempt(Long deliveryId, MailDeliveryStatus status, String errorType, String errorMessage);

    /**
     * 将投递记录标记为已发送。
     *
     * @param deliveryId 投递记录 ID
     */
    void markSent(Long deliveryId);
}
