package com.ccsanjuu.blog.modules.notification.model.vo;

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
public class NotificationItemVO {

    private Long id;

    private NotificationType type;

    private String title;

    private String content;

    private Boolean read;

    private OffsetDateTime createdAt;
}
