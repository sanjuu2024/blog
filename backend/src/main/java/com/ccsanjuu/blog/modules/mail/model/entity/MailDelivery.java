package com.ccsanjuu.blog.modules.mail.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccsanjuu.blog.modules.mail.model.enums.MailDeliveryStatus;
import com.ccsanjuu.blog.modules.mail.model.enums.MailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blog_mail_delivery")
public class MailDelivery {

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

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
