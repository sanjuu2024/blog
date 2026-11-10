package com.ccsanjuu.blog.modules.article.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveArticleVO {

    private Long id;

    private String title;

    private OffsetDateTime publishedAt;

    private ArticleCategoryVO category;
}
