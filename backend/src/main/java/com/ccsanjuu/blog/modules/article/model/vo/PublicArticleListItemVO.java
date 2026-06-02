package com.ccsanjuu.blog.modules.article.model.vo;

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
public class PublicArticleListItemVO {

    private Long id;

    private String title;

    private String summary;

    private String coverUrl;

    private Boolean isTop;

    private OffsetDateTime publishedAt;

    private Integer viewCount;

    private ArticleCategoryVO category;

    private List<ArticleTagVO> tags;
}
