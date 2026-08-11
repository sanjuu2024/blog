package com.ccsanjuu.blog.modules.article.model.bo;

import com.ccsanjuu.blog.modules.article.model.entity.Article;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 公开文章全文检索结果，额外携带 PostgreSQL 生成的标题和摘要或正文高亮片段。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PublicArticleSearchBO extends Article {

    private String highlightedTitle;

    private String searchSnippet;
}
