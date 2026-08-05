package com.ccsanjuu.blog.modules.comment.model.dto;

import com.ccsanjuu.blog.common.api.PageQuery;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentType;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminCommentQueryDTO extends PageQuery {

    @Positive(message = "文章 ID 必须大于 0")
    private Long articleId;

    @Positive(message = "用户 ID 必须大于 0")
    private Long userId;

    private CommentStatus status;

    private CommentType type;

    private OffsetDateTime createdAtFrom;

    private OffsetDateTime createdAtTo;
}
