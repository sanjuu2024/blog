package com.ccsanjuu.blog.modules.message.model.vo;

import com.ccsanjuu.blog.modules.message.model.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicMessageItemVO {

    private Long id;

    private String nickname;

    private String content;

    private MessageStatus status;

    private String moderationReason;

    private MessageAuthorVO author;

    private Boolean isMine;

    private List<MessageReplyVO> replies;

    private OffsetDateTime createdAt;
}
