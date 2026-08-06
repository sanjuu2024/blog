package com.ccsanjuu.blog.modules.comment.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 顶层评论回复数量。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentReplyCountBO {

    private Long rootId;

    private Long replyCount;

    private Long visibleReplyCount;
}
