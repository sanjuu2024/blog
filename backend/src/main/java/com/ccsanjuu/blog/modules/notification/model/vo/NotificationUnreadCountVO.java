package com.ccsanjuu.blog.modules.notification.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationUnreadCountVO {

    private Long total;

    private Long reply;

    private Long adminMessage;
}
