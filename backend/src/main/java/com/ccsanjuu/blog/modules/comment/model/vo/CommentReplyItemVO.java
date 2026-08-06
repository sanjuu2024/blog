package com.ccsanjuu.blog.modules.comment.model.vo;

import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReplyItemVO {

    private Long id;

    private Long articleId;

    private Long parentId;

    private Long rootId;

    private String content;

    private CommentStatus status;

    private String moderationReason;

    private CommentAuthorVO author;

    private CommentAuthorVO replyToUser;

    private Long replyCount;

    private Boolean hasVisibleReplies;

    private Boolean isMine;

    private OffsetDateTime createdAt;
}
