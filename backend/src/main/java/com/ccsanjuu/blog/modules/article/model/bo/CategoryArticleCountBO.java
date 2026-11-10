package com.ccsanjuu.blog.modules.article.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryArticleCountBO {

    private Long categoryId;

    private Long articleCount;
}
