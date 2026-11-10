package com.ccsanjuu.blog.modules.notification.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationStatus;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationTargetScope;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blog_notification")
public class Notification {

    private Long id;

    private NotificationType type;

    private NotificationTargetScope targetScope;

    private Long selectedUserId;

    private String title;

    private String content;

    private NotificationStatus status;

    private String sourceType;

    private Long sourceId;

    private Long createdBy;

    private OffsetDateTime publishedAt;

    private OffsetDateTime offlineAt;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
