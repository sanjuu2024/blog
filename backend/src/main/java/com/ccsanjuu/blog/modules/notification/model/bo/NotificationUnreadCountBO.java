package com.ccsanjuu.blog.modules.notification.model.bo;

import lombok.Data;

@Data
public class NotificationUnreadCountBO {

    private Long total;

    private Long reply;

    private Long adminMessage;
}
