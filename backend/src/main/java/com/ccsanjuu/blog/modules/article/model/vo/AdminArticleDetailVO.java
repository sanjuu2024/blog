package com.ccsanjuu.blog.modules.article.model.vo;

import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
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
public class AdminArticleDetailVO {

    private Long id;

    private String title;

    private String summary;

    private String contentMd;

    private String contentHtml;

    private String contentText;

    private String coverUrl;

    private ArticleStatus status;

    private Boolean isTop;

    private Long categoryId;

    private List<Long> tagIds;

    private Boolean allowComment;

    private OffsetDateTime publishedAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
