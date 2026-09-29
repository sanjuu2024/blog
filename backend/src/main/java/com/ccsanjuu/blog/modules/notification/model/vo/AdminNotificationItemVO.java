package com.ccsanjuu.blog.modules.notification.model.vo;

import com.ccsanjuu.blog.modules.notification.model.enums.NotificationStatus;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationTargetScope;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationItemVO {

    private Long id;

    private NotificationType type;

    private NotificationTargetScope targetScope;

    private Long userId;

    private String title;

    private String content;

    private NotificationStatus status;

    private Long createdBy;

    private OffsetDateTime publishedAt;

    private OffsetDateTime createdAt;
}
