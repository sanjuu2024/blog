package com.ccsanjuu.blog.modules.comment.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentLikeMutationVO {

    private Boolean liked;

    private Integer likeCount;
}
