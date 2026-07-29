package com.ccsanjuu.blog.modules.comment.model.vo;

import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCommentItemVO {

    private Long id;

    private Long articleId;

    private Long parentId;

    private Long rootId;

    private String content;

    private CommentStatus status;

    private String moderationReason;

    private CommentAuthorVO author;

    private Long reviewedBy;

    private OffsetDateTime reviewedAt;

    private Long deletedBy;

    private OffsetDateTime deletedAt;

    private OffsetDateTime createdAt;

    private AdminCommentArticleVO article;

    private CommentType type;
}
