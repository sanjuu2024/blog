package com.ccsanjuu.blog.modules.article.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 复用文章保存规则生成的安全 HTML，不保存预览记录。
 */
@Data
@AllArgsConstructor
public class ArticlePreviewVO {

    private String contentHtml;
}
