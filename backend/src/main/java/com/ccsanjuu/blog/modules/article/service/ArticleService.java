package com.ccsanjuu.blog.modules.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.article.model.dto.AdminArticleQueryDTO;
import com.ccsanjuu.blog.modules.article.model.dto.ArticleUpsertRequestDTO;
import com.ccsanjuu.blog.modules.article.model.dto.PublicArticleQueryDTO;
import com.ccsanjuu.blog.modules.article.model.dto.UpdateArticleStatusRequestDTO;
import com.ccsanjuu.blog.modules.article.model.bo.ArticleViewIdentity;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.vo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

public interface ArticleService extends IService<Article> {
    /**
     * 获取后台文章分页列表
     *
     * @param adminArticleQueryDTO
     * @return
     */
    PageResult<AdminArticleListItemVO> getArticleList(@Valid AdminArticleQueryDTO adminArticleQueryDTO);

    /**
     * 获取后台文章详情
     *
     * @param articleId
     * @return
     */
    AdminArticleDetailVO getArticleDetail(@Valid @Positive Long articleId);

    /**
     * 创建文章
     *
     * @param userId
     * @param articleUpsertRequestDTO
     * @return
     */
    CreatedArticleVO createArticle(Long userId, ArticleUpsertRequestDTO articleUpsertRequestDTO);

    /**
     * 更新文章
     *
     * @param articleId
     * @param userId
     * @param articleUpsertRequestDTO
     * @return
     */
    UpdatedArticleVO updateArticle(Long articleId, Long userId, @Valid ArticleUpsertRequestDTO articleUpsertRequestDTO);

    /**
     * 删除文章
     *
     * @param userId
     * @param articleId
     */
    void deleteArticle(Long userId, @Positive Long articleId);

    /**
     * 修改文章状态
     *
     * @param articleId
     * @param userId
     * @param updateArticleStatusRequestDTO
     * @return
     */
    UpdatedArticleStatusVO updateArticleStatus(@Positive Long articleId, Long userId, UpdateArticleStatusRequestDTO updateArticleStatusRequestDTO);

    /**
     * 获取已发布文章分页列表
     *
     * @param queryDTO
     * @return
     */
    PageResult<PublicArticleListItemVO> getPublicArticleList(@Valid PublicArticleQueryDTO queryDTO);

    /**
     * 获取前台文章详情
     *
     * @param articleId
     * @return
     */
    PublicArticleDetailVO getPublicArticleDetail(Long articleId, ArticleViewIdentity viewIdentity);
}
