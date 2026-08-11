package com.ccsanjuu.blog.modules.article.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    /**
     * 搜索时返回的安全标题高亮 HTML；标题未命中或未搜索时不序列化。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String highlightedTitle;

    /**
     * 搜索时返回的安全摘要或正文高亮片段；未搜索或仅标题命中时不序列化，
     * 避免首页和分类页收到无意义的 searchSnippet: null。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String searchSnippet;

    private String coverUrl;

    private Boolean isTop;

    private OffsetDateTime publishedAt;

    private Integer viewCount;

    private ArticleCategoryVO category;

    private List<ArticleTagVO> tags;
}
