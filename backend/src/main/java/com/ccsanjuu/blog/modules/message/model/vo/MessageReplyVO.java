package com.ccsanjuu.blog.modules.message.model.vo;

import com.ccsanjuu.blog.modules.message.model.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReplyVO {

    private Long id;

    private Long parentId;

    private String content;

    private MessageStatus status;

    private MessageAuthorVO author;

    private OffsetDateTime createdAt;
}
