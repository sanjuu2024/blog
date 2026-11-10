package com.ccsanjuu.blog.modules.notification.model.dto;

import com.ccsanjuu.blog.common.api.PageQuery;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationType;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationQueryDTO extends PageQuery {

    @Pattern(regexp = "ALL|REPLY|ADMIN_MESSAGE")
    private String category;

    private NotificationType type;
}
