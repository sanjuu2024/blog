package com.ccsanjuu.blog.modules.notification.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.modules.notification.model.bo.NotificationItemBO;
import com.ccsanjuu.blog.modules.notification.model.bo.NotificationUnreadCountBO;
import com.ccsanjuu.blog.modules.notification.model.entity.Notification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {


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
     * 查询管理员消息列表。
     *
     * @return 管理员消息
     */
    List<Notification> selectAdminList();

    /**
     * 锁定管理员消息，串行化草稿编辑和状态变更。
     *
     * @param notificationId 通知 ID
     * @return 通知数据
     */
    Notification selectNotificationByIdForUpdate(@Param("notificationId") Long notificationId);

}
