package com.ccsanjuu.blog.modules.article.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.mapper.ArticleTagMapper;
import com.ccsanjuu.blog.modules.article.model.dto.AdminArticleQueryDTO;
import com.ccsanjuu.blog.modules.article.model.dto.ArticleUpsertRequestDTO;
import com.ccsanjuu.blog.modules.article.model.dto.UpdateArticleStatusRequestDTO;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.entity.ArticleTag;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.article.model.vo.*;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.ccsanjuu.blog.modules.article.support.ArticleContentRenderer;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.tag.model.entity.Tag;
import com.ccsanjuu.blog.modules.tag.model.enums.TagStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private final ArticleTagMapper articleTagMapper;
    private final ArticleContentRenderer articleContentRenderer;
    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;
    private final TagMapper tagMapper;

    /**
     * 获取后台文章分页列表
     *
     * @param adminArticleQueryDTO
     * @return
     */
    @Override
    public PageResult<AdminArticleListItemVO> getArticleList(AdminArticleQueryDTO adminArticleQueryDTO) {
        Page<Article> page = Page.of(adminArticleQueryDTO.getPageNum(), adminArticleQueryDTO.getPageSize());

        String likeTitle = "";
        boolean hasText = StringUtils.hasText(adminArticleQueryDTO.getTitle());
        if (hasText) {
            likeTitle = "%"+adminArticleQueryDTO.getTitle().trim().toLowerCase()+"%";
        }

        // 🔺查询分类时，支持按照 一级 / 二级 分类查询，所以这里需要额外处理下分类的查询条件
        List<Long> categoryIds = List.of();
        if (adminArticleQueryDTO.getCategoryId() != null) {
             Category category = categoryMapper.selectById(adminArticleQueryDTO.getCategoryId());

             if (category == null) {
                 throw new BizException(ResultCode.ARTICLE_CATEGORY_NOT_FOUND);
             }

             categoryIds = category.getLevel() == 2
                            ? List.of(category.getId())
                            : categoryMapper.selectList(
                                new LambdaQueryWrapper<Category>()
                                        .eq(Category::getParentId, category.getId())
                            ).stream().map(Category::getId).toList();
        }


        lambdaQuery()
                .apply(hasText, "LOWER(title) like {0}", likeTitle)
                .in(adminArticleQueryDTO.getCategoryId() != null, Article::getCategoryId, categoryIds)
                .eq(adminArticleQueryDTO.getStatus() != null, Article::getStatus, adminArticleQueryDTO.getStatus())
                .eq(adminArticleQueryDTO.getIsTop() != null, Article::getIsTop, adminArticleQueryDTO.getIsTop())
                .orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getCreatedAt)
                .page(page);

        List<AdminArticleListItemVO> res = new ArrayList<>();
        page.getRecords().forEach(article -> {
            AdminArticleListItemVO vo = BeanUtil.copyProperties(article, AdminArticleListItemVO.class);

            Category category = categoryMapper.selectById(article.getCategoryId());
            ArticleCategoryVO categoryVo = BeanUtil.copyProperties(category, ArticleCategoryVO.class);
            Category parentCategory = categoryMapper.selectById(category.getParentId());
            categoryVo.setParent(BeanUtil.copyProperties(parentCategory, ArticleCategoryParentVO.class));

            vo.setCategory(categoryVo);
            res.add(vo);
        });

        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), res);
    }


    /**
     * 获取后台文章详情
     *
     * @param articleId
     * @return
     */
    @Override
    public AdminArticleDetailVO getArticleDetail(Long articleId) {
        Article article = getById(articleId);
        if (article == null) {
            throw new BizException(ResultCode.ARTICLE_NOT_FOUND);
        }
        AdminArticleDetailVO res =  BeanUtil.copyProperties(article, AdminArticleDetailVO.class);

        List<Long> tagIds = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>()
                        .select(ArticleTag::getTagId)
                        .eq(ArticleTag::getArticleId, articleId)
        ).stream().map(ArticleTag::getTagId).toList();   // 🔺🔺🔺只要 tagId！使用流转换 map。

        res.setTagIds(tagIds);

        return res;
    }


    /**
     * 创建文章
     *
     * @param userId
     * @param articleUpsertRequestDTO
     * @return
     */
    @Override
    @Transactional
    public CreatedArticleVO createArticle(Long userId, ArticleUpsertRequestDTO articleUpsertRequestDTO) {
        // 1. 校验、基础字段填入补充
        Article article = upsertArticleCommonOperation(articleUpsertRequestDTO);

        // 2. 填入作者（即当前登录用户）
        article.setAuthorId(userId);

        // 3. 插入并获取插入后的数据对象
        articleMapper.insert(article);
        Article newArticle = articleMapper.selectById(article.getId());

        // 4. 插入该文章和已选标签的关联关系
        checkAndInsertArticleTags(article.getId(), articleUpsertRequestDTO.getTagIds());

        // 5. 返回 vo
        return BeanUtil.copyProperties(newArticle, CreatedArticleVO.class);
    }


    /**
     * 更新文章
     *
     * @param articleId
     * @param userId
     * @param articleUpsertRequestDTO
     * @return
     */
    @Override
    @Transactional
    public UpdatedArticleVO updateArticle(Long articleId, Long userId, ArticleUpsertRequestDTO articleUpsertRequestDTO) {
        // 1. 校验文章是否存在和操作权限
        Article oldArticle = getById(articleId);
        if (oldArticle == null){
            throw new BizException(ResultCode.ARTICLE_NOT_FOUND);
        }
        if (!oldArticle.getAuthorId().equals(userId)){
            throw new BizException(ResultCode.NO_PERMISSION);
        }

        // 2. 校验、填入基础字段
        Article article = upsertArticleCommonOperation(articleUpsertRequestDTO);
        article.setId(articleId);

        // 3. 更新文章
        articleMapper.updateById(article);

        // 4. 更新文章和标签的关联关系（删掉旧的，插入新的）
        deleteArticleTags(articleId);
        checkAndInsertArticleTags(articleId, articleUpsertRequestDTO.getTagIds());

        // 5. 返回 vo
        Article newArticle = articleMapper.selectById(articleId);
        return BeanUtil.copyProperties(newArticle, UpdatedArticleVO.class);
    }


    /**
     * 删除文章
     *
     * @param userId
     * @param articleId
     */
    @Override
    @Transactional
    public void deleteArticle(Long userId, Long articleId) {
        Article article = getById(articleId);

        if (article == null) {
            throw new BizException(ResultCode.ARTICLE_NOT_FOUND);
        }

        if (!article.getAuthorId().equals(userId)){
            throw new BizException(ResultCode.NO_PERMISSION);
        }

        // 记得清除该文章和已选标签的关联关系
        deleteArticleTags(articleId);
        articleMapper.deleteById(articleId);
    }


    /**
     * 修改文章状态
     *
     * @param articleId
     * @param updateArticleStatusRequestDTO
     * @return
     */
    @Override
    public UpdatedArticleStatusVO updateArticleStatus(Long articleId, UpdateArticleStatusRequestDTO updateArticleStatusRequestDTO) {
        checkArticleExists(articleId);

        lambdaUpdate()
                .eq(Article::getId, articleId)
                .set(Article::getStatus, updateArticleStatusRequestDTO.getStatus())
                .set(updateArticleStatusRequestDTO.getStatus() == ArticleStatus.PUBLISHED, Article::getPublishedAt, OffsetDateTime.now())
                .update();

        Article article = getById(articleId);
        return BeanUtil.copyProperties(article, UpdatedArticleStatusVO.class);
    }



    /**
     * 校验文章分类合法性
     *
     * @param categoryId
     */
    private void checkArticleCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BizException(ResultCode.ARTICLE_CATEGORY_NOT_FOUND);
        }
        if (category.getLevel() == 1){
            throw new BizException(ResultCode.ARTICLE_CATEGORY_LEVEL_INVALID);
        }
        if (category.getStatus() == CategoryStatus.DISABLED){
            throw new BizException(ResultCode.ARTICLE_CATEGORY_DISABLED);
        }
    }


    /**
     * 创建和更新文章的共同操作：
     * 1. 校验分类合法性
     * 2. 判断是否需要填入发表时间
     * 3. 渲染文章内容
     *
     * @param articleUpsertRequestDTO
     * @return 基本填充完毕的文章对象
     */
    private Article upsertArticleCommonOperation(ArticleUpsertRequestDTO articleUpsertRequestDTO) {
        // 1. 校验分类合法性
        checkArticleCategory(articleUpsertRequestDTO.getCategoryId());

        // 2. 判断是否需要填入发表时间
        Article article = BeanUtil.copyProperties(articleUpsertRequestDTO, Article.class);
        if (articleUpsertRequestDTO.getStatus() == ArticleStatus.PUBLISHED){
            article.setPublishedAt(OffsetDateTime.now());
        }

        // 3. 渲染文章内容
        String html = articleContentRenderer.convertMarkdownToHtml(articleUpsertRequestDTO.getContentMd());
        String text = articleContentRenderer.convertToText(articleUpsertRequestDTO.getContentMd());
        article.setContentHtml(html);
        article.setContentText(text);

        return article;
    }


    /**
     * 校验操作目标文章是否存在
     *
     * @param articleId
     */
    private Article checkArticleExists(Long articleId) {
        Article article = getById(articleId);
        if (article == null) {
            throw new BizException(ResultCode.ARTICLE_NOT_FOUND);
        }

        return article;
    }

    /**
     * 删除目标文章关联的标签联系
     *
     * @param articleId
     */
    private void deleteArticleTags(Long articleId) {
        articleTagMapper.delete(
                new LambdaQueryWrapper<ArticleTag>()
                        .eq(ArticleTag::getArticleId, articleId)
        );
    }

    /**
     * 校验标签合法性并插入文章与标签的关联
     *
     * @param articleId
     * @param tagIds
     */
    private void checkAndInsertArticleTags(Long articleId, List<Long> tagIds) {
        if (CollectionUtil.isEmpty(tagIds)){
            return;
        }

        // 存在
        List<Long> distinctTagIds = tagIds.stream()
                .distinct()
                .toList();

        List<Tag> tags = tagMapper.selectByIds(distinctTagIds);
        if (tags.isEmpty() || tags.size() != distinctTagIds.size()) {
            throw new BizException(ResultCode.ARTICLE_TAG_NOT_FOUND);
        }

        // 未被禁用
        tags.forEach(tag -> {
           if (tag.getStatus() == TagStatus.DISABLED){
               throw new BizException(ResultCode.ARTICLE_TAG_DISABLED);
           }
        });

        // 插入关联关系
        articleTagMapper.insertBatch(articleId, distinctTagIds);
    }
}
