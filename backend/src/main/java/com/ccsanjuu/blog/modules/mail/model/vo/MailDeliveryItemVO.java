package com.ccsanjuu.blog.modules.mail.model.vo;

import com.ccsanjuu.blog.modules.mail.model.enums.MailDeliveryStatus;
import com.ccsanjuu.blog.modules.mail.model.enums.MailType;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class MailDeliveryItemVO {

    private Long id;

    private MailType mailType;

    private Long sourceId;

    private Long replyId;

    private String recipientMasked;

    private MailDeliveryStatus status;

    private Integer attemptCount;

    private String lastErrorType;

    private String lastErrorMessage;

    private OffsetDateTime sentAt;
    
    private OffsetDateTime lastAttemptAt;
    
    private OffsetDateTime createdAt;
}
