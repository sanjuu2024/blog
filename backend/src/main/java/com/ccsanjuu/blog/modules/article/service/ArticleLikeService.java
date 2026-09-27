package com.ccsanjuu.blog.modules.article.service;

import com.ccsanjuu.blog.modules.article.model.bo.ArticleLikeIdentity;
import com.ccsanjuu.blog.modules.article.model.vo.ArticleLikeMutationVO;

public interface ArticleLikeService {

    /**
     * 为文章增加当前主体的点赞。
     *
     * @param articleId 文章 ID
     * @param identity 登录用户或游客身份
     * @return 点赞状态和最新点赞数
     */
    ArticleLikeMutationVO likeArticle(Long articleId, ArticleLikeIdentity identity);

    /**
     * 取消当前主体对文章的点赞。
     *
     * @param articleId 文章 ID
     * @param identity 登录用户或游客身份
     * @return 点赞状态和最新点赞数
     */
    ArticleLikeMutationVO unlikeArticle(Long articleId, ArticleLikeIdentity identity);

    /**
     * 查询当前主体是否已点赞文章。
     *
     * @param articleId 文章 ID
     * @param identity 登录用户或游客身份
     * @return 是否已点赞
     */
    boolean isLiked(Long articleId, ArticleLikeIdentity identity);
}
