package com.ccsanjuu.blog.modules.notification.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blog_notification_recipient")
public class NotificationRecipient {

    private Long notificationId;

    private Long userId;

    private OffsetDateTime readAt;

    private OffsetDateTime createdAt;
}
