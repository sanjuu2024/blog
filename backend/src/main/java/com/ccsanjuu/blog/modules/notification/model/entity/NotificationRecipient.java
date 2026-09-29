package com.ccsanjuu.blog.modules.notification.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableField(fill = FieldFill.UPDATE)
    private OffsetDateTime readAt;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

}
