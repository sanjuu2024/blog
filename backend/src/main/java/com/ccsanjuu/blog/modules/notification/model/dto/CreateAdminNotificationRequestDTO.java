package com.ccsanjuu.blog.modules.notification.model.dto;

import com.ccsanjuu.blog.modules.notification.model.enums.NotificationTargetScope;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateAdminNotificationRequestDTO {

    @NotNull
    private NotificationTargetScope targetScope;

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String content;

    private List<Long> userIds;

    private NotificationStatus status = NotificationStatus.PUBLISHED;
}
