package com.ccsanjuu.blog.modules.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.constants.ValidationConstants;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.message.model.dto.CreateMessageReplyRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.CreateMessageRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessageBatchApprovalRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessageModerationRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessagePageQueryDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessageUnsubscribeRequestDTO;
import com.ccsanjuu.blog.modules.message.model.entity.Message;
import com.ccsanjuu.blog.modules.message.model.enums.MessageModerationAction;
import com.ccsanjuu.blog.modules.message.model.enums.MessageStatus;
import com.ccsanjuu.blog.modules.message.model.enums.MessageType;
import com.ccsanjuu.blog.modules.message.model.vo.AdminMessageItemVO;
import com.ccsanjuu.blog.modules.message.model.vo.MessageAuthorVO;
import com.ccsanjuu.blog.modules.message.model.vo.MessageMutationVO;
import com.ccsanjuu.blog.modules.message.model.vo.MessageReplyVO;
import com.ccsanjuu.blog.modules.message.model.vo.PublicMessageItemVO;
import com.ccsanjuu.blog.modules.message.service.MessageReplyNotificationService;
import com.ccsanjuu.blog.modules.message.service.MessageService;
import com.ccsanjuu.blog.modules.message.support.MessageRateLimiter;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final MessageRateLimiter messageRateLimiter;
    private final MessageReplyNotificationService notificationService;

    /**
     * 获取公开留言分页列表，并在每条顶层留言下附带管理员回复。
     *
     * @param currentUserId 当前用户 ID，游客为空
     * @param queryDTO 分页参数
     * @return 公共留言分页结果
     */
    @Override
    public PageResult<PublicMessageItemVO> getPublicMessageList(Long currentUserId, MessagePageQueryDTO queryDTO) {
        Page<Message> page = Page.of(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .isNull(Message::getParentId)
                .orderByDesc(Message::getCreatedAt)
                .orderByDesc(Message::getId);
        addPublicVisibility(wrapper, currentUserId);
        messageMapper.selectPage(page, wrapper);

        List<Message> roots = page.getRecords();
        if (roots.isEmpty()) {
            return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), List.of());
        }

        List<Long> rootIds = roots.stream().map(Message::getId).toList();
        List<Message> replies = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .in(Message::getParentId, rootIds)
                .eq(Message::getStatus, MessageStatus.APPROVED)
                .orderByAsc(Message::getCreatedAt)
                .orderByAsc(Message::getId));
        Map<Long, List<Message>> repliesByRoot = replies.stream()
                .collect(Collectors.groupingBy(Message::getParentId));
        Map<Long, User> users = getUserMap(roots, replies);

        List<PublicMessageItemVO> records = roots.stream()
                .map(root -> buildPublicMessage(root, repliesByRoot.getOrDefault(root.getId(), List.of()), users, currentUserId))
                .toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    /**
     * 创建游客或登录用户的顶层留言。
     *
     * @param currentUserId 当前用户 ID，游客为空
     * @param clientIp 请求客户端地址
     * @param requestDTO 创建参数
     * @return 创建结果
     */
    @Override
    @Transactional
    public MessageMutationVO createMessage(Long currentUserId, String clientIp, CreateMessageRequestDTO requestDTO) {
        User user = currentUserId == null ? null : getActiveUser(currentUserId);
        boolean admin = user != null && user.getRole() == UserRole.ADMIN;
        String content = requestDTO.getContent().trim();
        boolean notify = Boolean.TRUE.equals(requestDTO.getNotifyOnReply());

        String nickname;
        String email;
        if (user == null) {
            nickname = trimGuestNickname(requestDTO.getNickname());
            email = trimGuestEmail(requestDTO.getEmail());
            if (notify && !StringUtils.hasText(email)) {
                throw new BizException(ResultCode.MESSAGE_EMAIL_REQUIRED);
            }
            if (!admin) {
                messageRateLimiter.acquire("ip:" + (StringUtils.hasText(clientIp) ? clientIp : "unknown"));
            }
        } else {
            nickname = user.getNickname();
            email = trim(user.getEmail());
            if (!admin) {
                messageRateLimiter.acquire("user:" + user.getId());
            }
        }

        Message message = Message.builder()
                .userId(user == null ? null : user.getId())
                .nickname(nickname)
                .email(email == null ? "" : email)
                .content(content)
                .status(admin ? MessageStatus.APPROVED : MessageStatus.PENDING)
                .notifyOnReply(notify && StringUtils.hasText(email))
                .unsubscribeToken(notify && StringUtils.hasText(email) ? UUID.randomUUID().toString() : null)
                .build();
        messageMapper.insert(message);
        return buildMutation(messageMapper.selectById(message.getId()), user, currentUserId);
    }

    /**
     * 删除登录用户自己的顶层留言及其管理员回复。
     *
     * @param messageId 留言 ID
     * @param userId 当前用户 ID
     */
    @Override
    @Transactional
    public void deleteOwnMessage(Long messageId, Long userId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null || message.getStatus() == MessageStatus.DELETED) {
            throw new BizException(ResultCode.MESSAGE_NOT_FOUND);
        }
        if (!userId.equals(message.getUserId()) || message.getParentId() != null) {
            throw new BizException(ResultCode.MESSAGE_NO_PERMISSION);
        }

        Message locked = lockMessageTree(message);
        if (locked.getStatus() == MessageStatus.DELETED) {
            throw new BizException(ResultCode.MESSAGE_NOT_FOUND);
        }
        deleteMessageSubtree(locked, userId, null);
    }

    /**
     * 获取后台留言分页列表。
     *
     * @param queryDTO 查询条件
     * @return 后台留言分页结果
     */
    @Override
    public PageResult<AdminMessageItemVO> getAdminMessageList(MessagePageQueryDTO queryDTO) {
        Page<Message> page = Page.of(queryDTO.getPageNum(), queryDTO.getPageSize());
        boolean guestFilter = StringUtils.hasText(queryDTO.getGuestNickname())
                || StringUtils.hasText(queryDTO.getGuestEmail());
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(queryDTO.getMessageId() != null, Message::getId, queryDTO.getMessageId())
                .eq(queryDTO.getUserId() != null, Message::getUserId, queryDTO.getUserId())
                .isNull(guestFilter, Message::getUserId)
                .like(StringUtils.hasText(queryDTO.getGuestNickname()), Message::getNickname, trim(queryDTO.getGuestNickname()))
                .like(StringUtils.hasText(queryDTO.getGuestEmail()), Message::getEmail, trim(queryDTO.getGuestEmail()))
                .like(StringUtils.hasText(queryDTO.getContent()), Message::getContent, trim(queryDTO.getContent()))
                .eq(queryDTO.getStatus() != null, Message::getStatus, queryDTO.getStatus())
                .isNull(queryDTO.getType() == MessageType.TOP_LEVEL, Message::getParentId)
                .isNotNull(queryDTO.getType() == MessageType.REPLY, Message::getParentId)
                .ge(queryDTO.getCreatedAtFrom() != null, Message::getCreatedAt, queryDTO.getCreatedAtFrom())
                .le(queryDTO.getCreatedAtTo() != null, Message::getCreatedAt, queryDTO.getCreatedAtTo())
                .orderByDesc(Message::getCreatedAt)
                .orderByDesc(Message::getId);
        messageMapper.selectPage(page, wrapper);

        List<Message> records = page.getRecords();
        Map<Long, User> users = getUserMap(records, List.of());
        List<AdminMessageItemVO> result = records.stream()
                .map(message -> buildAdminMessage(
                        message,
                        message.getUserId() == null ? null : users.get(message.getUserId())
                ))
                .toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), result);
    }

    /**
     * 审核、隐藏或删除留言。
     *
     * @param messageId 留言 ID
     * @param adminId 管理员 ID
     * @param requestDTO 审核参数
     * @return 处理后的留言
     */
    @Override
    @Transactional
    public MessageMutationVO moderateMessage(Long messageId, Long adminId, MessageModerationRequestDTO requestDTO) {
        getActiveAdmin(adminId);
        Message current = messageMapper.selectById(messageId);
        if (current == null) {
            throw new BizException(ResultCode.MESSAGE_NOT_FOUND);
        }
        Message locked = lockMessageTree(current);
        MessageModerationAction action = requestDTO.getAction();
        if (!canModerate(locked.getStatus(), action)) {
            throw new BizException(ResultCode.MESSAGE_STATUS_TRANSITION_INVALID);
        }

        String reason = requestDTO.getReason();
        if (action != MessageModerationAction.APPROVE && !StringUtils.hasText(reason)) {
            throw new BizException(ResultCode.MESSAGE_MODERATION_REASON_REQUIRED);
        }
        OffsetDateTime now = now();
        if (action == MessageModerationAction.DELETE) {
            deleteMessageSubtree(locked, adminId, reason.trim());
        } else {
            MessageStatus target = toStatus(action);
            int rows = messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                    .eq(Message::getId, messageId)
                    .eq(Message::getStatus, locked.getStatus())
                    .set(Message::getStatus, target)
                    .set(Message::getModerationReason, action == MessageModerationAction.APPROVE ? null : reason.trim())
                    .set(Message::getReviewedBy, adminId)
                    .set(Message::getReviewedAt, now)
                    .set(Message::getUpdatedAt, now));
            if (rows != 1) {
                throw new BizException(ResultCode.MESSAGE_STATUS_TRANSITION_INVALID);
            }
        }
        Message result = messageMapper.selectById(messageId);
        return buildMutation(result, findUser(result), null);
    }

    /**
     * 管理员对已通过的顶层留言发表回复，并在事务提交后触发通知邮件。
     *
     * @param messageId 顶层留言 ID
     * @param adminId 管理员 ID
     * @param requestDTO 回复参数
     * @return 回复结果
     */
    @Override
    @Transactional
    public MessageMutationVO replyMessage(Long messageId, Long adminId, CreateMessageReplyRequestDTO requestDTO) {
        User admin = getActiveAdmin(adminId);
        Message root = messageMapper.selectByIdForUpdate(messageId);
        if (root == null || root.getParentId() != null || root.getStatus() != MessageStatus.APPROVED) {
            throw new BizException(ResultCode.MESSAGE_PARENT_UNAVAILABLE);
        }

        Message reply = Message.builder()
                .userId(admin.getId())
                .parentId(root.getId())
                .nickname(admin.getNickname())
                .email("")
                .content(requestDTO.getContent().trim())
                .status(MessageStatus.APPROVED)
                .notifyOnReply(false)
                .build();
        messageMapper.insert(reply);
        notificationService.sendAfterCommit(root, reply);
        return buildMutation(messageMapper.selectById(reply.getId()), admin, admin.getId());
    }

    /**
     * 原子批量通过待审核顶层留言。
     *
     * @param adminId 管理员 ID
     * @param requestDTO 留言 ID 列表
     */
    @Override
    @Transactional
    public void approveMessages(Long adminId, MessageBatchApprovalRequestDTO requestDTO) {
        getActiveAdmin(adminId);
        List<Long> ids = requestDTO.getMessageIds();
        if (ids.size() > 100) {
            throw new BizException(ResultCode.MESSAGE_BATCH_TOO_LARGE);
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0) || new HashSet<>(ids).size() != ids.size()) {
            throw new BizException(ResultCode.MESSAGE_BATCH_INVALID);
        }
        List<Message> messages = messageMapper.selectPendingRootsForUpdate(ids);
        if (messages.size() != ids.size()) {
            throw new BizException(ResultCode.MESSAGE_BATCH_INVALID);
        }
        OffsetDateTime now = now();
        int rows = messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .in(Message::getId, ids)
                .isNull(Message::getParentId)
                .eq(Message::getStatus, MessageStatus.PENDING)
                .set(Message::getStatus, MessageStatus.APPROVED)
                .set(Message::getModerationReason, null)
                .set(Message::getReviewedBy, adminId)
                .set(Message::getReviewedAt, now)
                .set(Message::getUpdatedAt, now));
        if (rows != ids.size()) {
            throw new BizException(ResultCode.MESSAGE_BATCH_INVALID);
        }
    }

    /**
     * 幂等关闭单条顶层留言的后续回复通知。
     *
     * @param requestDTO 退订令牌
     */
    @Override
    @Transactional
    public void unsubscribe(MessageUnsubscribeRequestDTO requestDTO) {
        Message message = messageMapper.selectOne(new LambdaQueryWrapper<Message>()
                .eq(Message::getUnsubscribeToken, requestDTO.getToken().trim())
                .isNull(Message::getParentId));
        if (message == null) {
            throw new BizException(ResultCode.MESSAGE_UNSUBSCRIBE_TOKEN_INVALID);
        }
        if (Boolean.TRUE.equals(message.getNotifyOnReply())) {
            messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                    .eq(Message::getId, message.getId())
                    .set(Message::getNotifyOnReply, false)
                    .set(Message::getUpdatedAt, now()));
        }
    }

    private void addPublicVisibility(LambdaQueryWrapper<Message> wrapper, Long currentUserId) {
        wrapper.and(query -> query.eq(Message::getStatus, MessageStatus.APPROVED)
                .or(currentUserId != null, mine -> mine.eq(Message::getUserId, currentUserId)
                        .in(Message::getStatus, MessageStatus.PENDING, MessageStatus.REJECTED)));
    }

    private Map<Long, User> getUserMap(List<Message> first, List<Message> second) {
        Set<Long> ids = new HashSet<>();
        first.stream().map(Message::getUserId).filter(id -> id != null).forEach(ids::add);
        second.stream().map(Message::getUserId).filter(id -> id != null).forEach(ids::add);
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectByIds(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private MessageAuthorVO toAuthor(User user) {
        return user == null ? null : MessageAuthorVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private PublicMessageItemVO buildPublicMessage(
            Message message,
            List<Message> replies,
            Map<Long, User> users,
            Long currentUserId
    ) {
        boolean mine = currentUserId != null && currentUserId.equals(message.getUserId());
        return PublicMessageItemVO.builder()
                .id(message.getId())
                .nickname(message.getNickname())
                .content(message.getContent())
                .status(message.getStatus())
                .moderationReason(mine && message.getStatus() == MessageStatus.REJECTED ? message.getModerationReason() : null)
                .author(toAuthor(message.getUserId() == null ? null : users.get(message.getUserId())))
                .isMine(mine)
                .replies(replies.stream().map(reply -> MessageReplyVO.builder()
                        .id(reply.getId())
                        .parentId(reply.getParentId())
                        .content(reply.getContent())
                        .status(reply.getStatus())
                        .author(toAuthor(users.get(reply.getUserId())))
                        .createdAt(reply.getCreatedAt())
                        .build()).toList())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private AdminMessageItemVO buildAdminMessage(Message message, User user) {
        return AdminMessageItemVO.builder()
                .id(message.getId())
                .userId(message.getUserId())
                .parentId(message.getParentId())
                .nickname(message.getNickname())
                .email(message.getEmail())
                .content(message.getContent())
                .status(message.getStatus())
                .type(message.getParentId() == null ? MessageType.TOP_LEVEL : MessageType.REPLY)
                .notifyOnReply(Boolean.TRUE.equals(message.getNotifyOnReply()))
                .moderationReason(message.getModerationReason())
                .reviewedBy(message.getReviewedBy())
                .reviewedAt(message.getReviewedAt())
                .deletedBy(message.getDeletedBy())
                .deletedAt(message.getDeletedAt())
                .author(toAuthor(user))
                .createdAt(message.getCreatedAt())
                .build();
    }

    private MessageMutationVO buildMutation(Message message, User user, Long currentUserId) {
        boolean mine = currentUserId != null && message != null && currentUserId.equals(message.getUserId());
        return MessageMutationVO.builder()
                .id(message.getId())
                .parentId(message.getParentId())
                .nickname(message.getNickname())
                .content(message.getContent())
                .status(message.getStatus())
                .moderationReason(mine && message.getStatus() == MessageStatus.REJECTED ? message.getModerationReason() : null)
                .author(toAuthor(user))
                .isMine(mine)
                .replies(List.of())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private User findUser(Message message) {
        return message == null || message.getUserId() == null ? null : userMapper.selectById(message.getUserId());
    }

    private User getActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        return user;
    }

    private User getActiveAdmin(Long userId) {
        User user = getActiveUser(userId);
        if (user.getRole() != UserRole.ADMIN) {
            throw new BizException(ResultCode.NO_PERMISSION);
        }
        return user;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimGuestEmail(String email) {
        String value = trim(email);
        if (StringUtils.hasText(value)
                && (value.length() > 255 || !value.matches(ValidationConstants.EMAIL_PATTERN))) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
        return value;
    }

    private String trimGuestNickname(String nickname) {
        String value = trim(nickname);
        if (!StringUtils.hasText(value) || value.length() > 20
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new BizException(ResultCode.MESSAGE_NICKNAME_INVALID);
        }
        return value;
    }

    private boolean canModerate(MessageStatus status, MessageModerationAction action) {
        return switch (action) {
            case APPROVE -> status == MessageStatus.PENDING
                    || status == MessageStatus.REJECTED
                    || status == MessageStatus.HIDDEN;
            case REJECT -> status == MessageStatus.PENDING;
            case HIDE -> status == MessageStatus.APPROVED;
            case DELETE -> status != MessageStatus.DELETED;
        };
    }

    private MessageStatus toStatus(MessageModerationAction action) {
        return switch (action) {
            case APPROVE -> MessageStatus.APPROVED;
            case REJECT -> MessageStatus.REJECTED;
            case HIDE -> MessageStatus.HIDDEN;
            case DELETE -> MessageStatus.DELETED;
        };
    }

    private Message lockMessageTree(Message message) {
        Long rootId = message.getParentId() == null ? message.getId() : message.getParentId();
        Message root = messageMapper.selectByIdForUpdate(rootId);
        if (root == null) {
            throw new BizException(ResultCode.MESSAGE_NOT_FOUND);
        }
        return message.getParentId() == null ? root : messageMapper.selectById(message.getId());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void deleteMessageSubtree(Message message, Long operatorId, String reason) {
        if (message == null) {
            throw new BizException(ResultCode.MESSAGE_NOT_FOUND);
        }
        List<Long> ids = messageMapper.selectSubtreeIds(message.getId());
        if (ids.isEmpty()) {
            throw new BizException(ResultCode.MESSAGE_NOT_FOUND);
        }
        OffsetDateTime now = now();
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .in(Message::getId, ids)
                .ne(Message::getStatus, MessageStatus.DELETED)
                .set(Message::getStatus, MessageStatus.DELETED)
                .set(Message::getModerationReason, reason == null ? null : reason.trim())
                .set(Message::getDeletedBy, operatorId)
                .set(Message::getDeletedAt, now)
                .set(Message::getUpdatedAt, now));
    }
}
