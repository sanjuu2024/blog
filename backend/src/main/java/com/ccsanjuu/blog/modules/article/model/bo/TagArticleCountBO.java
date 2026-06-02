package com.ccsanjuu.blog.modules.article.model.bo;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagArticleCountBO {

    private Long tagId;

    private Long articleCount;
}
