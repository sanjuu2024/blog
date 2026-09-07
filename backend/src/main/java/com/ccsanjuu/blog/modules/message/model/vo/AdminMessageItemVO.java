package com.ccsanjuu.blog.modules.message.model.vo;

import com.ccsanjuu.blog.modules.message.model.enums.MessageStatus;
import com.ccsanjuu.blog.modules.message.model.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMessageItemVO {

    private Long id;

    private Long userId;

    private Long parentId;

    private String nickname;

    private String email;

    private String content;

    private MessageStatus status;

    private MessageType type;

    private Boolean notifyOnReply;

    private String moderationReason;

    private Long reviewedBy;

    private OffsetDateTime reviewedAt;

    private Long deletedBy;

    private OffsetDateTime deletedAt;

    private MessageAuthorVO author;

    private OffsetDateTime createdAt;
}
