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
public class PublicArticleDetailVO {

    private Long id;

    private String title;

    private String summary;

    private String contentHtml;

    private String coverUrl;

    private Boolean isTop;

    private Boolean allowComment;

    private Integer viewCount;

    private Integer commentCount;

    private Integer likeCount;

    private Integer favoriteCount;

    private OffsetDateTime publishedAt;

    private OffsetDateTime updatedAt;

    private ArticleCategoryVO category;

    private List<ArticleTagVO> tags;

    private ArticleAuthorVO author;
}
