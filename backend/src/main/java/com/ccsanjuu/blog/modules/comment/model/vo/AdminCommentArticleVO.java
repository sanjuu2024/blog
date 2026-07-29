package com.ccsanjuu.blog.modules.comment.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCommentArticleVO {

    private Long id;

    private String title;
}
