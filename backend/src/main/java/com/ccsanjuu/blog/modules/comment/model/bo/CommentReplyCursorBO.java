package com.ccsanjuu.blog.modules.comment.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReplyCursorBO {

    /**
     * 管理员直接回复顶层评论的优先级，置顶回复为 0，其他回复为 1。
     */
    private Integer directAdminReplyPriority;

    private OffsetDateTime createdAt;

    private Long id;
}
