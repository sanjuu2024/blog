package com.ccsanjuu.blog.modules.notification.model.bo;

import com.ccsanjuu.blog.modules.notification.model.enums.NotificationType;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class NotificationItemBO {

    private Long id;

    private NotificationType type;

    private String title;

    private String content;

    private Boolean read;

    private OffsetDateTime createdAt;
}
