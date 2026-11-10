package com.ccsanjuu.blog.modules.article.model.vo;

import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminArticleListItemVO {

    private Long id;

    private String title;

    private String summary;

    private ArticleStatus status;

    private Boolean isTop;

    private String coverUrl;

    private OffsetDateTime publishedAt;

    private OffsetDateTime updatedAt;

    private ArticleCategoryVO category;
}
