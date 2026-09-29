package com.ccsanjuu.blog.modules.notification.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.notification.mapper.NotificationMapper;
import com.ccsanjuu.blog.modules.notification.mapper.NotificationRecipientMapper;
import com.ccsanjuu.blog.modules.notification.model.bo.NotificationItemBO;
import com.ccsanjuu.blog.modules.notification.model.bo.NotificationUnreadCountBO;
import com.ccsanjuu.blog.modules.notification.model.dto.CreateAdminNotificationRequestDTO;
import com.ccsanjuu.blog.modules.notification.model.dto.NotificationQueryDTO;
import com.ccsanjuu.blog.modules.notification.model.dto.UpdateAdminNotificationRequestDTO;
import com.ccsanjuu.blog.modules.notification.model.entity.Notification;
import com.ccsanjuu.blog.modules.notification.model.entity.NotificationRecipient;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationStatus;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationTargetScope;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationType;
import com.ccsanjuu.blog.modules.notification.model.vo.AdminNotificationItemVO;
import com.ccsanjuu.blog.modules.notification.model.vo.NotificationItemVO;
import com.ccsanjuu.blog.modules.notification.model.vo.NotificationUnreadCountVO;
import com.ccsanjuu.blog.modules.notification.service.NotificationService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationRecipientMapper notificationRecipientMapper;
    private final UserMapper userMapper;
    private final TransactionTemplate replyNotificationTransaction;

    /**
     * 初始化回复通知独立事务。
     *
     * @param notificationMapper 通知持久层
     * @param userMapper 用户持久层
     * @param transactionManager 事务管理器
     */
    public NotificationServiceImpl(
            NotificationMapper notificationMapper,
            NotificationRecipientMapper notificationRecipientMapper,
            UserMapper userMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.notificationMapper = notificationMapper;
        this.notificationRecipientMapper = notificationRecipientMapper;
        this.userMapper = userMapper;
        this.replyNotificationTransaction = new TransactionTemplate(transactionManager);
        this.replyNotificationTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 获取当前用户通知分页列表。
     *
     * @param userId 当前用户 ID
     * @param queryDTO 查询条件
     * @return 通知分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<NotificationItemVO> getUserNotifications(Long userId, NotificationQueryDTO queryDTO) {
        validateCategory(queryDTO.getCategory());
        Page<NotificationItemBO> page = Page.of(queryDTO.getPageNum(), queryDTO.getPageSize());
        notificationMapper.selectUserPage(page, userId, queryDTO.getCategory(),
                queryDTO.getType() == null ? null : queryDTO.getType().name());
        List<NotificationItemVO> records = page.getRecords().stream()
                .map(item -> NotificationItemVO.builder()
                        .id(item.getId())
                        .type(item.getType())
                        .title(item.getTitle())
                        .content(item.getContent())
                        .sourceId(item.getSourceId())
                        .authorName(item.getAuthorName())
                        .originalContent(item.getOriginalContent())
                        .articleId(item.getArticleId())
                        .parentId(item.getParentId())
                        .likeCount(item.getLikeCount())
                        .liked(item.getLiked())
                        .canInteract(item.getCanInteract())
                        .read(Boolean.TRUE.equals(item.getRead()))
                        .createdAt(item.getCreatedAt())
                        .build())
                .toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    /**
     * 获取当前用户未读数量。
     *
     * @param userId 当前用户 ID
     * @return 未读数量
     */
    @Override
    @Transactional(readOnly = true)
    public NotificationUnreadCountVO getUnreadCount(Long userId) {
        NotificationUnreadCountBO count = notificationMapper.selectUnreadCount(userId);
        return NotificationUnreadCountVO.builder()
                .total(count == null || count.getTotal() == null ? 0L : count.getTotal())
                .reply(count == null || count.getReply() == null ? 0L : count.getReply())
                .adminMessage(count == null || count.getAdminMessage() == null ? 0L : count.getAdminMessage())
                .build();
    }

    /**
     * 标记单条通知已读。
     *
     * @param userId 当前用户 ID
     * @param notificationId 通知 ID
     */
    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || notification.getStatus() != NotificationStatus.PUBLISHED) {
            return;
        }
        notificationRecipientMapper.update(
                NotificationRecipient.builder().build(),
                new LambdaUpdateWrapper<NotificationRecipient>()
                        .eq(NotificationRecipient::getNotificationId, notificationId)
                        .eq(NotificationRecipient::getUserId, userId)
                        .isNull(NotificationRecipient::getReadAt)
        );
    }

    /**
     * 标记通知全部已读或按分类标记已读。
     *
     * @param userId 当前用户 ID
     * @param category 通知分类
     */
    @Override
    @Transactional
    public void markAllRead(Long userId, String category) {
        validateCategory(category);
        notificationRecipientMapper.selectIdsForRead(userId, category).forEach(notificationId ->
                notificationRecipientMapper.update(
                        NotificationRecipient.builder().build(),
                        new LambdaUpdateWrapper<NotificationRecipient>()
                                .eq(NotificationRecipient::getNotificationId, notificationId)
                                .eq(NotificationRecipient::getUserId, userId)
                                .isNull(NotificationRecipient::getReadAt)
                )
        );
    }

    /**
     * 创建管理员消息草稿或直接发布；草稿暂不向全部用户生成收件记录。
     *
     * @param adminId 管理员 ID
     * @param requestDTO 创建参数
     * @return 创建结果
     */
    @Override
    @Transactional
    public AdminNotificationItemVO createAdminMessage(Long adminId, CreateAdminNotificationRequestDTO requestDTO) {
        Long userId = validateTargetScope(requestDTO.getTargetScope(), requestDTO.getUserId());
        validateSelectedUser(userId);
        NotificationStatus status = requestDTO.getStatus() == null ? NotificationStatus.PUBLISHED : requestDTO.getStatus();
        if (status == NotificationStatus.OFFLINE) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
        Notification notification = Notification.builder()
                .type(NotificationType.ADMIN_MESSAGE)
                .targetScope(requestDTO.getTargetScope())
                .selectedUserId(userId)
                .title(requestDTO.getTitle().trim())
                .content(requestDTO.getContent().trim())
                .status(status)
                .createdBy(adminId)
                .build();
        notificationMapper.insert(notification);
        if (requestDTO.getTargetScope() == NotificationTargetScope.ALL_USERS) {
            if (status == NotificationStatus.PUBLISHED) {
                insertRecipientsForAllUsers(notification.getId(), adminId);
            }
        } else if (status == NotificationStatus.PUBLISHED) {
            insertSelectedRecipient(notification.getId(), userId, adminId);
        }
        return toAdminVO(notificationMapper.selectNotificationByIdForUpdate(notification.getId()));
    }

    /**
     * 获取管理员消息列表。
     *
     * @return 管理员消息列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<AdminNotificationItemVO> getAdminMessages() {
        return notificationMapper.selectAdminList().stream().map(this::toAdminVO).toList();
    }

    /**
     * 更新管理员消息草稿。
     *
     * @param notificationId 通知 ID
     * @param requestDTO 更新参数
     * @return 更新结果
     */
    @Override
    @Transactional
    public AdminNotificationItemVO updateAdminMessage(Long notificationId, UpdateAdminNotificationRequestDTO requestDTO) {
        Notification current = requireAdminMessage(notificationId);
        if (current.getStatus() != NotificationStatus.DRAFT) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
        Long userId = validateTargetScope(requestDTO.getTargetScope(), requestDTO.getUserId());
        validateSelectedUser(userId);
        current.setTargetScope(requestDTO.getTargetScope());
        current.setSelectedUserId(userId);
        current.setTitle(requestDTO.getTitle().trim());
        current.setContent(requestDTO.getContent().trim());
        if (notificationMapper.updateById(current) != 1) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return toAdminVO(current);
    }

    /**
     * 修改管理员消息状态。
     *
     * @param notificationId 通知 ID
     * @param status 目标状态
     * @return 更新结果
     */
    @Override
    @Transactional
    public AdminNotificationItemVO updateAdminMessageStatus(Long notificationId, String status) {
        Notification current = requireAdminMessage(notificationId);
        NotificationStatus target;
        try {
            target = NotificationStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
        if (target == NotificationStatus.DRAFT || target == current.getStatus()
                || (current.getStatus() == NotificationStatus.DRAFT && target == NotificationStatus.OFFLINE)) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
        if (current.getStatus() == NotificationStatus.DRAFT) {
            if (current.getTargetScope() == NotificationTargetScope.ALL_USERS) {
                insertRecipientsForAllUsers(notificationId, current.getCreatedBy());
            } else {
                insertSelectedRecipient(notificationId, current.getSelectedUserId(), current.getCreatedBy());
            }
        }
        current.setStatus(target);
        current.setPublishedAt(target == NotificationStatus.PUBLISHED && current.getPublishedAt() == null
                ? java.time.OffsetDateTime.now() : current.getPublishedAt());
        current.setOfflineAt(target == NotificationStatus.OFFLINE ? java.time.OffsetDateTime.now() : current.getOfflineAt());
        if (notificationMapper.updateById(current) != 1) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return toAdminVO(current);
    }

    /**
     * 创建评论回复通知。
     *
     * @param recipientUserId 收件用户 ID
     * @param sourceId 回复评论 ID
     * @param title 通知标题
     * @param content 通知正文
     */
    @Override
    @Transactional
    public void createCommentReplyNotification(Long recipientUserId, Long sourceId, String title, String content) {
        createReplyNotification(NotificationType.COMMENT_REPLY, recipientUserId, sourceId, title, content);
    }

    /**
     * 创建留言回复通知。
     *
     * @param recipientUserId 收件用户 ID
     * @param sourceId 回复留言 ID
     * @param title 通知标题
     * @param content 通知正文
     */
    @Override
    @Transactional
    public void createMessageReplyNotification(Long recipientUserId, Long sourceId, String title, String content) {
        createReplyNotification(NotificationType.MESSAGE_REPLY, recipientUserId, sourceId, title, content);
    }

    /**
     * 业务提交后在独立事务中创建回复通知，通知失败不回滚评论或留言。
     *
     * @param type 回复通知类型
     * @param recipientUserId 收件用户 ID
     * @param sourceId 回复 ID
     * @param title 通知标题
     * @param content 通知正文
     */
    private void createReplyNotification(
            NotificationType type,
            Long recipientUserId,
            Long sourceId,
            String title,
            String content
    ) {
        if (recipientUserId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    persistReplyNotification(type, recipientUserId, sourceId, title, content);
                }
            });
        } else {
            persistReplyNotification(type, recipientUserId, sourceId, title, content);
        }
    }

    /**
     * 在新事务中持久化回复通知并隔离失败。
     *
     * @param type 回复通知类型
     * @param recipientUserId 收件用户 ID
     * @param sourceId 回复 ID
     * @param title 通知标题
     * @param content 通知正文
     */
    private void persistReplyNotification(
            NotificationType type, Long recipientUserId, Long sourceId, String title, String content
    ) {
        try {
            replyNotificationTransaction.executeWithoutResult(status -> insertReplyNotification(
                    type, recipientUserId, sourceId, title, content));
        } catch (RuntimeException exception) {
            log.error("站内回复通知创建失败: type={} sourceId={} reason={}",
                    type, sourceId, exception.getClass().getSimpleName());
        }
    }

    /**
     * 写入回复通知及其收件记录。
     *
     * @param type 回复通知类型
     * @param recipientUserId 收件用户 ID
     * @param sourceId 回复 ID
     * @param title 通知标题
     * @param content 通知正文
     */
    private void insertReplyNotification(
            NotificationType type, Long recipientUserId, Long sourceId, String title, String content
    ) {
        Notification notification = Notification.builder()
                .type(type)
                .targetScope(NotificationTargetScope.SELECTED_USERS)
                .title(title)
                .content(content)
                .status(NotificationStatus.PUBLISHED)
                .sourceType(type == NotificationType.COMMENT_REPLY ? "COMMENT" : "MESSAGE")
                .sourceId(sourceId)
                .build();
        notificationMapper.insert(notification);
        insertRecipientsForUsers(notification.getId(), recipientUserId, null);
    }

    /**
     * 校验消息收件范围并返回指定用户 ID。
     *
     * @param scope 收件范围
     * @param userId 指定用户 ID
     * @return 校验后的指定用户 ID
     */
    private Long validateTargetScope(NotificationTargetScope scope, Long userId) {
        if (scope == NotificationTargetScope.SELECTED_USERS && (userId == null || userId <= 0)) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
        if (scope == NotificationTargetScope.ALL_USERS && userId != null) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
        return userId;
    }

    /**
     * 校验指定用户仍可作为管理员消息收件人。
     *
     * @param userId 指定用户 ID
     */
    private void validateSelectedUser(Long userId) {
        if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user == null || user.getDeletedAt() != null) {
                throw new BizException(ResultCode.PARAM_INVALID);
            }
        }
    }

    /**
     * 所有指定用户均须存在且未被逻辑删除，避免消息成功但部分用户未收到。
     *
     * @param notificationId 通知 ID
     * @param userId 指定用户 ID
     */
    private void insertSelectedRecipient(Long notificationId, Long userId, Long excludedUserId) {
        int expectedRows = userId.equals(excludedUserId) ? 0 : 1;
        if (insertRecipientsForUsers(notificationId, userId, excludedUserId) != expectedRows) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
    }

    /**
     * 向指定用户生成收件记录。
     *
     * @param notificationId 通知 ID
     * @param userId 用户 ID
     * @param excludedUserId 排除的用户 ID
     * @return 插入数量
     */
    private int insertRecipientsForUsers(Long notificationId, Long userId, Long excludedUserId) {
        if (userId.equals(excludedUserId) || userMapper.selectById(userId) == null) {
            return 0;
        }
        return notificationRecipientMapper.insert(NotificationRecipient.builder()
                .notificationId(notificationId)
                .userId(userId)
                .build());
    }

    /**
     * 向全部未逻辑删除用户生成收件记录。
     *
     * @param notificationId 通知 ID
     * @param excludedUserId 排除的用户 ID
     */
    private void insertRecipientsForAllUsers(Long notificationId, Long excludedUserId) {
        List<?> userIds = userMapper.selectObjs(new QueryWrapper<User>()
                .select("id")
                .isNull("deleted_at")
                .ne(excludedUserId != null, "id", excludedUserId));
        if (userIds == null) {
            return;
        }
        userIds.forEach(userId -> notificationRecipientMapper.insert(NotificationRecipient.builder()
                .notificationId(notificationId)
                .userId(((Number) userId).longValue())
                .build()));
    }

    /**
     * 校验通知分类，避免未知分类被解释为全部已读。
     *
     * @param category 通知分类
     */
    private void validateCategory(String category) {
        if (category != null && !List.of("ALL", "REPLY", "ADMIN_MESSAGE").contains(category)) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
    }

    /**
     * 查询并锁定管理员消息。
     *
     * @param notificationId 通知 ID
     * @return 管理员消息
     */
    private Notification requireAdminMessage(Long notificationId) {
        Notification notification = notificationMapper.selectNotificationByIdForUpdate(notificationId);
        if (notification == null || notification.getType() != NotificationType.ADMIN_MESSAGE) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return notification;
    }

    /**
     * 组装管理员消息响应。
     *
     * @param notification 管理员消息
     * @return 响应数据
     */
    private AdminNotificationItemVO toAdminVO(Notification notification) {
        return AdminNotificationItemVO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .targetScope(notification.getTargetScope())
                .userId(notification.getTargetScope() == NotificationTargetScope.SELECTED_USERS
                        ? notification.getSelectedUserId() : null)
                .title(notification.getTitle())
                .content(notification.getContent())
                .status(notification.getStatus())
                .createdBy(notification.getCreatedBy())
                .publishedAt(notification.getPublishedAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
