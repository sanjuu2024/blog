package com.ccsanjuu.blog.modules.article.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.util.TokenHashUtil;
import com.ccsanjuu.blog.modules.article.mapper.ArticleLikeMapper;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.bo.ArticleLikeIdentity;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.entity.ArticleLike;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.article.model.vo.ArticleLikeMutationVO;
import com.ccsanjuu.blog.modules.article.service.ArticleLikeService;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ArticleLikeServiceImpl implements ArticleLikeService {

    private final ArticleMapper articleMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final CategoryMapper categoryMapper;

    /**
     * 为文章增加当前主体的点赞。
     *
     * @param articleId 文章 ID
     * @param identity 登录用户或游客身份
     * @return 点赞状态和最新点赞数
     */
    @Override
    @Transactional
    public ArticleLikeMutationVO likeArticle(Long articleId, ArticleLikeIdentity identity) {
        requirePublicArticle(articleId);
        Actor actor = resolveActor(identity);
        ArticleLike existingLike = articleLikeMapper.selectByActor(
                articleId,
                actor.userId(),
                actor.visitorTokenHash()
        );
        if (existingLike == null
                && articleLikeMapper.insertIgnore(ArticleLike.builder()
                .articleId(articleId)
                .userId(actor.userId())
                .visitorTokenHash(actor.visitorTokenHash())
                .build()) > 0) {
            articleMapper.incrementLikeCount(articleId);
        }
        return mutation(true, articleId);
    }

    /**
     * 取消当前主体对文章的点赞。
     *
     * @param articleId 文章 ID
     * @param identity 登录用户或游客身份
     * @return 点赞状态和最新点赞数
     */
    @Override
    @Transactional
    public ArticleLikeMutationVO unlikeArticle(Long articleId, ArticleLikeIdentity identity) {
        requireArticle(articleId);
        Actor actor = resolveActor(identity);
        if (articleLikeMapper.deleteByActor(
                articleId,
                actor.userId(),
                actor.visitorTokenHash()
        ) > 0) {
            articleMapper.decrementLikeCount(articleId);
        }
        return mutation(false, articleId);
    }

    /**
     * 查询当前主体是否已点赞文章。
     *
     * @param articleId 文章 ID
     * @param identity 登录用户或游客身份
     * @return 是否已点赞
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isLiked(Long articleId, ArticleLikeIdentity identity) {
        Actor actor = resolveActor(identity);
        return articleLikeMapper.selectByActor(
                articleId,
                actor.userId(),
                actor.visitorTokenHash()
        ) != null;
    }

    private Article requirePublicArticle(Long articleId) {
        Article article = requireArticle(articleId);
        if (article.getStatus() != ArticleStatus.PUBLISHED) {
            throw new BizException(ResultCode.ARTICLE_NOT_VISIBLE);
        }
        Category category = categoryMapper.selectById(article.getCategoryId());
        if (category == null) {
            throw new BizException(ResultCode.ARTICLE_CATEGORY_NOT_FOUND);
        }
        if (category.getStatus() == CategoryStatus.DISABLED) {
            throw new BizException(ResultCode.ARTICLE_CATEGORY_DISABLED);
        }
        Category parentCategory = categoryMapper.selectById(category.getParentId());
        if (parentCategory == null) {
            throw new BizException(ResultCode.ARTICLE_CATEGORY_NOT_FOUND);
        }
        if (parentCategory.getStatus() == CategoryStatus.DISABLED) {
            throw new BizException(ResultCode.ARTICLE_CATEGORY_DISABLED);
        }
        return article;
    }

    private Article requireArticle(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BizException(ResultCode.ARTICLE_NOT_FOUND);
        }
        return article;
    }

    private ArticleLikeMutationVO mutation(boolean liked, Long articleId) {
        Article article = requireArticle(articleId);
        return ArticleLikeMutationVO.builder()
                .liked(liked)
                .likeCount(article.getLikeCount() == null ? 0 : article.getLikeCount())
                .build();
    }

    private Actor resolveActor(ArticleLikeIdentity identity) {
        if (identity == null
                || (!identity.isAuthenticated() && !StringUtils.hasText(identity.visitorToken()))) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
        return identity.isAuthenticated()
                ? new Actor(identity.userId(), null)
                : new Actor(null, TokenHashUtil.sha256(identity.visitorToken()));
    }

    private record Actor(Long userId, String visitorTokenHash) {
    }
}
