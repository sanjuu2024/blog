package com.ccsanjuu.blog.modules.article.service;

import com.ccsanjuu.blog.modules.article.model.bo.ArticleViewIdentity;

public interface ArticleViewService {

    /**
     * 记录文章有效浏览，并返回当前浏览总数。
     *
     * @param articleId 文章 ID
     * @param identity 登录用户或游客身份
     * @return 当前文章有效浏览总数
     */
    Integer recordView(Long articleId, ArticleViewIdentity identity);
}
