package com.ccsanjuu.blog.modules.mail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.mail.mapper.MailDeliveryMapper;
import com.ccsanjuu.blog.modules.mail.model.dto.MailDeliveryQueryDTO;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import com.ccsanjuu.blog.modules.mail.model.enums.MailDeliveryStatus;
import com.ccsanjuu.blog.modules.mail.model.enums.MailType;
import com.ccsanjuu.blog.modules.mail.model.vo.MailDeliveryItemVO;
import com.ccsanjuu.blog.modules.mail.service.MailDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ccsanjuu.blog.modules.message.service.MessageReplyNotificationService;
import com.ccsanjuu.blog.modules.comment.service.CommentReplyNotificationService;

import java.util.Locale;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class MailDeliveryServiceImpl implements MailDeliveryService {
    private final MailDeliveryMapper mailDeliveryMapper;
    private final ObjectProvider<MessageReplyNotificationService> messageReplyNotificationService;
    private final ObjectProvider<CommentReplyNotificationService> commentReplyNotificationService;

    /**
     * 查询邮件投递分页记录。
     *
     * @param queryDTO 查询条件和分页参数
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<MailDeliveryItemVO> getPage(MailDeliveryQueryDTO queryDTO) {
        Page<MailDelivery> page = Page.of(queryDTO.getPageNum(), queryDTO.getPageSize());
        mailDeliveryMapper.selectPage(page, new LambdaQueryWrapper<MailDelivery>()
                .eq(queryDTO.getMailType() != null, MailDelivery::getMailType, queryDTO.getMailType())
                .eq(queryDTO.getStatus() != null, MailDelivery::getStatus, queryDTO.getStatus())
                .ge(queryDTO.getCreatedAtFrom() != null, MailDelivery::getCreatedAt, queryDTO.getCreatedAtFrom())
                .le(queryDTO.getCreatedAtTo() != null, MailDelivery::getCreatedAt, queryDTO.getCreatedAtTo())
                .orderByDesc(MailDelivery::getCreatedAt)
                .orderByDesc(MailDelivery::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
                page.getRecords().stream().map(this::toVO).toList());
    }

    /**
     * 查询邮件投递详情。
     *
     * @param deliveryId 投递记录 ID
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public MailDeliveryItemVO getDetail(Long deliveryId) {
        MailDelivery delivery = mailDeliveryMapper.selectById(deliveryId);
        if (delivery == null) throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
        return toVO(delivery);
    }

    /**
     * 锁定失败记录并提交重新发送任务。
     *
     * @param deliveryId 投递记录 ID
     */
    @Override
    @Transactional
    public void retry(Long deliveryId) {
        MailDelivery delivery = mailDeliveryMapper.selectByIdForUpdate(deliveryId);
        if (delivery == null || delivery.getStatus() != MailDeliveryStatus.FAILED) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
        delivery.setStatus(MailDeliveryStatus.PENDING);
        delivery.setLastErrorType(null);
        delivery.setLastErrorMessage(null);
        mailDeliveryMapper.updateById(delivery);
        if (delivery.getMailType() == MailType.MESSAGE_REPLY) {
            messageReplyNotificationService.getObject().retry(delivery);
        } else if (delivery.getMailType() == MailType.COMMENT_REPLY) {
            commentReplyNotificationService.getObject().retry(delivery);
        }
    }

    /**
     * 创建待发送投递记录。
     *
     * @param mailType 邮件类型
     * @param sourceId 原始业务 ID
     * @param replyId 回复 ID
     * @param recipient 原始收件地址，仅用于生成脱敏地址
     * @return
     */
    @Override
    @Transactional
    public MailDelivery createPending(String mailType, Long sourceId, Long replyId, String recipient) {
        MailDelivery delivery = MailDelivery.builder()
                .mailType(MailType.valueOf(mailType.toUpperCase(Locale.ROOT)))
                .sourceId(sourceId)
                .replyId(replyId)
                .recipientMasked(maskRecipient(recipient))
                .status(MailDeliveryStatus.PENDING)
                .attemptCount(0)
                .build();
        mailDeliveryMapper.insert(delivery);
        return delivery;
    }

    /**
     * 更新一次失败尝试及其安全错误摘要。
     *
     * @param deliveryId 投递记录 ID
     * @param status 投递状态
     * @param errorType 安全错误类型
     * @param errorMessage 安全错误摘要
     */
    @Override
    @Transactional
    public void markAttempt(Long deliveryId, MailDeliveryStatus status, String errorType, String errorMessage) {
        MailDelivery delivery = mailDeliveryMapper.selectById(deliveryId);
        if (delivery == null) return;
        delivery.setStatus(status);
        delivery.setAttemptCount((delivery.getAttemptCount() == null ? 0 : delivery.getAttemptCount()) + 1);
        delivery.setLastAttemptAt(OffsetDateTime.now(ZoneOffset.UTC));
        delivery.setLastErrorType(errorType);
        delivery.setLastErrorMessage(errorMessage == null ? null : errorMessage.substring(0, Math.min(255, errorMessage.length())));
        mailDeliveryMapper.updateById(delivery);
    }

    /**
     * 将投递记录标记为已发送。
     *
     * @param deliveryId 投递记录 ID
     */
    @Override
    @Transactional
    public void markSent(Long deliveryId) {
        MailDelivery delivery = mailDeliveryMapper.selectById(deliveryId);
        if (delivery == null) return;
        delivery.setStatus(MailDeliveryStatus.SENT);
        delivery.setSentAt(OffsetDateTime.now(ZoneOffset.UTC));
        delivery.setLastAttemptAt(OffsetDateTime.now(ZoneOffset.UTC));
        delivery.setAttemptCount((delivery.getAttemptCount() == null ? 0 : delivery.getAttemptCount()) + 1);
        mailDeliveryMapper.updateById(delivery);
    }

    private MailDeliveryItemVO toVO(MailDelivery delivery) {
        return MailDeliveryItemVO.builder()
                .id(delivery.getId()).mailType(delivery.getMailType()).sourceId(delivery.getSourceId())
                .replyId(delivery.getReplyId()).recipientMasked(delivery.getRecipientMasked())
                .status(delivery.getStatus()).attemptCount(delivery.getAttemptCount())
                .lastErrorType(delivery.getLastErrorType()).lastErrorMessage(delivery.getLastErrorMessage())
                .sentAt(delivery.getSentAt()).lastAttemptAt(delivery.getLastAttemptAt())
                .createdAt(delivery.getCreatedAt()).build();
    }

    private String maskRecipient(String recipient) {
        if (recipient == null || recipient.isBlank()) return "***";
        int at = recipient.indexOf('@');
        if (at <= 0) return "***";
        String local = recipient.substring(0, at);
        return (local.length() <= 2 ? "*" : local.charAt(0) + "***") + recipient.substring(at);
    }
}
