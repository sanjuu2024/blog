package com.ccsanjuu.blog.modules.notification.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.modules.notification.model.bo.NotificationItemBO;
import com.ccsanjuu.blog.modules.notification.model.bo.NotificationUnreadCountBO;
import com.ccsanjuu.blog.modules.notification.model.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    /**
     * 查询用户可见通知。
     *
     * @param page 分页参数
     * @param userId 用户 ID
     * @param category 通知分类
     * @param type 通知类型
     * @return 通知分页结果
     */
    Page<NotificationItemBO> selectUserPage(
            Page<NotificationItemBO> page,
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("type") String type
    );

    /**
     * 查询用户未读通知数量。
     *
     * @param userId 用户 ID
     * @return 各分类未读数量
     */
    NotificationUnreadCountBO selectUnreadCount(@Param("userId") Long userId);

    /**
     * 标记当前用户的一条已发布通知为已读。
     *
     * @param notificationId 通知 ID
     * @param userId 用户 ID
     * @return 更新数量
     */
    int markRead(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    /**
     * 按分类标记用户通知为已读。
     *
     * @param userId 用户 ID
     * @param category 通知分类
     * @return 更新数量
     */
    int markAllRead(@Param("userId") Long userId, @Param("category") String category);

    /**
     * 查询管理员消息列表。
     *
     * @return 管理员消息
     */
    List<Notification> selectAdminList();

    /**
     * 新增通知。
     *
     * @param notification 通知数据
     * @return 插入数量
     */
    int insertNotification(Notification notification);

    /**
     * 向指定的启用用户生成收件记录。
     *
     * @param notificationId 通知 ID
     * @param userIds 用户 ID 列表
     * @return 插入数量
     */
    int insertRecipientsForUsers(
            @Param("notificationId") Long notificationId,
            @Param("userIds") List<Long> userIds
    );

    /**
     * 向当前全部启用用户生成收件记录。
     *
     * @param notificationId 通知 ID
     * @return 插入数量
     */
    int insertRecipientsForAllUsers(@Param("notificationId") Long notificationId);

    /**
     * 查询通知的指定收件用户。
     *
     * @param notificationId 通知 ID
     * @return 用户 ID 列表
     */
    List<Long> selectRecipientUserIds(@Param("notificationId") Long notificationId);

    /**
     * 锁定管理员消息，串行化草稿编辑和状态变更。
     *
     * @param notificationId 通知 ID
     * @return 通知数据
     */
    Notification selectNotificationByIdForUpdate(@Param("notificationId") Long notificationId);

    /**
     * 清除草稿的指定收件用户。
     *
     * @param notificationId 通知 ID
     * @return 删除数量
     */
    int deleteRecipients(@Param("notificationId") Long notificationId);

    /**
     * 更新管理员消息草稿。
     *
     * @param notification 通知数据
     * @return 更新数量
     */
    int updateNotification(Notification notification);

    /**
     * 更新管理员消息发布状态。
     *
     * @param notification 通知数据
     * @return 更新数量
     */
    int updateNotificationStatus(Notification notification);
}
