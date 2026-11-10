package com.ccsanjuu.blog.modules.notification.service;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.notification.model.dto.CreateAdminNotificationRequestDTO;
import com.ccsanjuu.blog.modules.notification.model.dto.NotificationQueryDTO;
import com.ccsanjuu.blog.modules.notification.model.dto.UpdateAdminNotificationRequestDTO;
import com.ccsanjuu.blog.modules.notification.model.vo.AdminNotificationItemVO;
import com.ccsanjuu.blog.modules.notification.model.vo.NotificationItemVO;
import com.ccsanjuu.blog.modules.notification.model.vo.NotificationUnreadCountVO;

import java.util.List;

public interface NotificationService {

    /**
     * 分页查询当前用户通知。
     *
     * @param userId 用户 ID
     * @param queryDTO 查询条件
     * @return 通知分页结果
     */
    PageResult<NotificationItemVO> getUserNotifications(Long userId, NotificationQueryDTO queryDTO);

    /**
     * 获取当前用户各分类未读数量。
     *
     * @param userId 用户 ID
     * @return 未读数量
     */
    NotificationUnreadCountVO getUnreadCount(Long userId);

    /**
     * 标记当前用户的一条通知为已读。
     *
     * @param userId 用户 ID
     * @param notificationId 通知 ID
     */
    void markRead(Long userId, Long notificationId);

    /**
     * 标记当前用户全部或指定分类通知为已读。
     *
     * @param userId 用户 ID
     * @param category 通知分类
     */
    void markAllRead(Long userId, String category);

    /**
     * 创建管理员消息草稿或直接发布。
     *
     * @param adminId 管理员 ID
     * @param requestDTO 消息内容和收件范围
     * @return 创建结果
     */
    AdminNotificationItemVO createAdminMessage(Long adminId, CreateAdminNotificationRequestDTO requestDTO);

    /**
     * 获取管理员消息列表。
     *
     * @return 管理员消息列表
     */
    List<AdminNotificationItemVO> getAdminMessages();

    /**
     * 更新管理员消息草稿。
     *
     * @param notificationId 通知 ID
     * @param requestDTO 消息内容和收件范围
     * @return 更新结果
     */
    AdminNotificationItemVO updateAdminMessage(Long notificationId, UpdateAdminNotificationRequestDTO requestDTO);

    /**
     * 发布或下线管理员消息。
     *
     * @param notificationId 通知 ID
     * @param status 目标状态
     * @return 更新结果
     */
    AdminNotificationItemVO updateAdminMessageStatus(Long notificationId, String status);

    /**
     * 创建评论直接回复通知。
     *
     * @param recipientUserId 收件用户 ID
     * @param sourceId 回复评论 ID
     * @param title 通知标题
     * @param content 通知正文
     */
    void createCommentReplyNotification(Long recipientUserId, Long sourceId, String title, String content);

    /**
     * 创建留言管理员回复通知。
     *
     * @param recipientUserId 收件用户 ID
     * @param sourceId 回复留言 ID
     * @param title 通知标题
     * @param content 通知正文
     */
    void createMessageReplyNotification(Long recipientUserId, Long sourceId, String title, String content);
}
