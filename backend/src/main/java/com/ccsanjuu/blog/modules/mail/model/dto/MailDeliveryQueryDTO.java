package com.ccsanjuu.blog.modules.mail.model.dto;

import com.ccsanjuu.blog.common.api.PageQuery;
import com.ccsanjuu.blog.modules.mail.model.enums.MailDeliveryStatus;
import com.ccsanjuu.blog.modules.mail.model.enums.MailType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class MailDeliveryQueryDTO extends PageQuery {
    
    private MailType mailType;

    private MailDeliveryStatus status;

    private OffsetDateTime createdAtFrom;

    private OffsetDateTime createdAtTo;
}
