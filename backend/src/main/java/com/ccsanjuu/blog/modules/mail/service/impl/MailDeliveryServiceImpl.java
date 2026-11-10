package com.ccsanjuu.blog.modules.mail.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
        Page<MailDeliveryItemVO> page = Page.of(queryDTO.getPageNum(), queryDTO.getPageSize());
        mailDeliveryMapper.selectAdminPage(page, queryDTO);
        return PageResult.of(page);
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
        MailDeliveryItemVO delivery = mailDeliveryMapper.selectAdminDetail(deliveryId);
        if (delivery == null) throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
        return delivery;
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
        mailDeliveryMapper.update(delivery, new LambdaUpdateWrapper<MailDelivery>()
                .eq(MailDelivery::getId, deliveryId)
                .set(MailDelivery::getLastErrorType, null)
                .set(MailDelivery::getLastErrorMessage, null));
        Runnable task = () -> {
            if (delivery.getMailType() == MailType.MESSAGE_REPLY) {
                messageReplyNotificationService.getObject().retry(delivery);
            } else if (delivery.getMailType() == MailType.COMMENT_REPLY) {
                commentReplyNotificationService.getObject().retry(delivery);
            }
        };
        // 先提交状态及审计，再启动任务，避免未提交时发信或事务回滚后仍发送。
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
     * 创建待发送投递记录。
     *
     * @param mailType 邮件类型
     * @param sourceId 原始业务 ID
     * @param replyId 回复 ID
     * @param recipient 完整收件地址
     * @param userId 收件用户 ID，游客为空
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MailDelivery createPending(String mailType, Long sourceId, Long replyId, String recipient, Long userId) {
        MailDelivery delivery = MailDelivery.builder()
                .mailType(MailType.valueOf(mailType.toUpperCase(Locale.ROOT)))
                .sourceId(sourceId)
                .replyId(replyId)
                .recipientEmail(recipient)
                .recipientUserId(userId)
                .status(MailDeliveryStatus.PENDING)
                .attemptCount(0)
                .build();
        mailDeliveryMapper.insert(delivery);
        return delivery;
    }

    /**
     * 在实际发送前保存本次收件目标。
     *
     * @param deliveryId 投递记录 ID
     * @param recipient 完整收件地址
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRecipient(Long deliveryId, String recipient) {
        mailDeliveryMapper.update(MailDelivery.builder().recipientEmail(recipient).build(),
                new LambdaUpdateWrapper<MailDelivery>().eq(MailDelivery::getId, deliveryId));
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long deliveryId) {
        MailDelivery delivery = mailDeliveryMapper.selectById(deliveryId);
        if (delivery == null) return;
        delivery.setStatus(MailDeliveryStatus.SENT);
        delivery.setSentAt(OffsetDateTime.now(ZoneOffset.UTC));
        delivery.setLastAttemptAt(OffsetDateTime.now(ZoneOffset.UTC));
        delivery.setAttemptCount((delivery.getAttemptCount() == null ? 0 : delivery.getAttemptCount()) + 1);
        mailDeliveryMapper.updateById(delivery);
    }
}
