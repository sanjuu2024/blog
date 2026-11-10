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
public class UpdatedArticleVO {

    private Long id;

    private ArticleStatus status;

    private OffsetDateTime publishedAt;

    private OffsetDateTime updatedAt;
}
