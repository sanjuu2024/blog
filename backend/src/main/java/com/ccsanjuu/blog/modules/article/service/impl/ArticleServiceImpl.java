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
import com.ccsanjuu.blog.modules.article.model.bo.PublicArticleSearchBO;
import com.ccsanjuu.blog.modules.article.model.dto.AdminArticleQueryDTO;
import com.ccsanjuu.blog.modules.article.model.dto.ArticleUpsertRequestDTO;
import com.ccsanjuu.blog.modules.article.model.dto.PublicArticleQueryDTO;
import com.ccsanjuu.blog.modules.article.model.dto.UpdateArticleStatusRequestDTO;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.entity.ArticleTag;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.article.model.enums.PublicArticleSort;
import com.ccsanjuu.blog.modules.article.model.vo.*;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.ccsanjuu.blog.modules.article.support.ArticleContentRenderer;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.tag.model.entity.Tag;
import com.ccsanjuu.blog.modules.tag.model.enums.TagStatus;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private static final String SEARCH_HIGHLIGHT_START = "__BLOG_SEARCH_HIGHLIGHT_START__";
    private static final String SEARCH_HIGHLIGHT_END = "__BLOG_SEARCH_HIGHLIGHT_END__";
    private static final String SEARCH_HIGHLIGHT_MARK = "<mark class=\"article-search-highlight\">";

    private final ArticleTagMapper articleTagMapper;
    private final ArticleContentRenderer articleContentRenderer;
    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;

    /**
     * 获取后台文章分页列表
     *
     * @param adminArticleQueryDTO
     * @return
     */
    @Override
    public PageResult<AdminArticleListItemVO> getArticleList(AdminArticleQueryDTO adminArticleQueryDTO) {
        // 1. 构建分页配置
        Page<Article> page = Page.of(adminArticleQueryDTO.getPageNum(), adminArticleQueryDTO.getPageSize());

        // 2. 清洗 title（trim、lowerCase）
        String likeTitle = "";
        boolean hasText = StringUtils.hasText(adminArticleQueryDTO.getTitle());
        if (hasText) {
            likeTitle = "%"+adminArticleQueryDTO.getTitle().trim().toLowerCase()+"%";
        }

        // 3. 🔺查询分类时，支持按照 一级 / 二级 分类查询，所以这里需要额外处理下分类的查询条件
        List<Long> queryCategoryIds = List.of();
        if (adminArticleQueryDTO.getCategoryId() != null) {
             Category category = categoryMapper.selectById(adminArticleQueryDTO.getCategoryId());

             if (category == null) {
                 throw new BizException(ResultCode.ARTICLE_CATEGORY_NOT_FOUND);
             }

             queryCategoryIds = category.getLevel() == 2
                            ? List.of(category.getId())
                            : categoryMapper.selectList(
                                new LambdaQueryWrapper<Category>()
                                        .eq(Category::getParentId, category.getId())
                            ).stream().map(Category::getId).toList();

             if (category.getLevel() == 1 && queryCategoryIds.isEmpty()){
                 // 查询的该一级分类下没有二级分类，查询结果一定为空
                 // 防止后续查询空的 queryCategoryIds 导致 SQL 错误（where category_id in ()），直接返回空结果
                 return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), List.of());
             }
        }

        // 4. 查询
        lambdaQuery()
                .apply(hasText, "LOWER(title) like {0}", likeTitle)
                .in(adminArticleQueryDTO.getCategoryId() != null, Article::getCategoryId, queryCategoryIds)
                .eq(adminArticleQueryDTO.getStatus() != null, Article::getStatus, adminArticleQueryDTO.getStatus())
                .eq(adminArticleQueryDTO.getIsTop() != null, Article::getIsTop, adminArticleQueryDTO.getIsTop())
                .orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getCreatedAt)
                .page(page);

        List<AdminArticleListItemVO> res = new ArrayList<>();

        // 5. 获取 vo 中的 ArticleCategoryVO 数据
        List<Article> records = page.getRecords();
        if (records.isEmpty()){
            // 防止后续查询空的 categoryIds 导致 SQL 错误（where id in ()），直接返回空结果
            return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), List.of());
        }

        // 注意 distinct() 对象是 Long id 而不是 Article 对象
        List<Long> categoryIds = records.stream().map(Article::getCategoryId).distinct().toList();
        Map<Long, ArticleCategoryVO> categoryVoMap = getCategoryVoMap(categoryIds);

        // 6. 封装返回
        records.forEach(article -> {
            AdminArticleListItemVO vo = BeanUtil.copyProperties(article, AdminArticleListItemVO.class);
            vo.setCategory(categoryVoMap.get(article.getCategoryId()));
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
        ).stream().map(ArticleTag::getTagId).toList();   // 🔺只要 tagId！使用流转换 map。

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
        if (articleUpsertRequestDTO.getStatus() == ArticleStatus.OFFLINE){
            // 创建文章的时候只能指定状态为 草稿 或者 已发布
            throw new BizException(ResultCode.ARTICLE_STATUS_TRANSITION_INVALID);
        }
        Article article = upsertArticleCommonOperation(null, articleUpsertRequestDTO);

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
        checkUpdateArticleStatus(oldArticle, articleUpsertRequestDTO.getStatus());
        Article article = upsertArticleCommonOperation(oldArticle, articleUpsertRequestDTO);
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
     * @param userId
     * @param updateArticleStatusRequestDTO
     * @return
     */
    @Override
    public UpdatedArticleStatusVO updateArticleStatus(Long articleId, Long userId, UpdateArticleStatusRequestDTO updateArticleStatusRequestDTO) {

        Article article = checkArticleExists(articleId);
        if (!article.getAuthorId().equals(userId)){
            throw new BizException(ResultCode.NO_PERMISSION);
        }

        checkUpdateArticleStatus(article, updateArticleStatusRequestDTO.getStatus());

        lambdaUpdate()
                .eq(Article::getId, articleId)
                .set(Article::getStatus, updateArticleStatusRequestDTO.getStatus())
                .set(updateArticleStatusRequestDTO.getStatus() == ArticleStatus.PUBLISHED && article.getPublishedAt() == null, Article::getPublishedAt, OffsetDateTime.now())
                .update();

        Article newArticle = getById(articleId);
        return BeanUtil.copyProperties(newArticle, UpdatedArticleStatusVO.class);
    }


    /**
     * 获取已发布文章分页列表
     *
     * @param queryDTO
     * @return
     */
    @Override
    public PageResult<PublicArticleListItemVO> getPublicArticleList(PublicArticleQueryDTO queryDTO) {
        // 1. 构建分页
        Page<Article> page = Page.of(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 校验分类条件合法性
        Category category = null;
        if (queryDTO.getCategoryId() != null){
            category = categoryMapper.selectById(queryDTO.getCategoryId());

            if (category == null){
                throw new BizException(ResultCode.ARTICLE_CATEGORY_NOT_FOUND);
            }

             if (category.getStatus() == CategoryStatus.DISABLED){
                 throw new BizException(ResultCode.ARTICLE_CATEGORY_DISABLED);
             }

             if (category.getLevel() == 2){
                 Category parentCategory = categoryMapper.selectById(category.getParentId());

                 if (parentCategory == null){
                     throw new BizException(ResultCode.ARTICLE_CATEGORY_NOT_FOUND);
                 }

                 if (parentCategory.getStatus() == CategoryStatus.DISABLED){
                     throw new BizException(ResultCode.ARTICLE_CATEGORY_DISABLED);
                 }
             }
        }

        // 3. 校验标签条件合法性
        List<Long> tagIds = List.of();
        if (!CollectionUtil.isEmpty(queryDTO.getTagIds())) {
            tagIds = queryDTO.getTagIds().stream().distinct().toList();
        }

        if (!tagIds.isEmpty()){
            List<Tag> tags = tagMapper.selectList(
                    new LambdaQueryWrapper<Tag>()
                            .in(Tag::getId, queryDTO.getTagIds().stream().distinct().toList())
            );

            if (tags == null || tags.size() != tagIds.size()){
                throw new BizException(ResultCode.ARTICLE_TAG_NOT_FOUND);
            }

            tags.forEach(tag -> {
                if (tag.getStatus() == TagStatus.DISABLED){
                    throw new BizException(ResultCode.ARTICLE_TAG_DISABLED);
                }
            });
        }

        // 4. 如果是一级分类，条件为其下所有二级分类的文章；如果是二级分类，直接查询
        List<Long> categoryIds = List.of();
        if (category != null){
            // 传了分类条件
            if (category.getLevel() == 1){
                categoryIds = categoryMapper.selectList(
                        new LambdaQueryWrapper<Category>()
                                .eq(Category::getParentId, category.getId())
                                .eq(Category::getStatus, CategoryStatus.ENABLED)
                ).stream().map(Category::getId).toList();
            }
            else {
                categoryIds = List.of(category.getId());
            }
        }
        else {
            // 不限分类
            // 即自己不被禁用且父分类也未被禁用的所有二级分类
            List<Category> categoryList = categoryMapper.selectList(
                    new LambdaQueryWrapper<Category>()
                            .eq(Category::getStatus, CategoryStatus.ENABLED)
            );
            Map<Long, Boolean> parentCategoryEnabled = categoryList.stream().filter(c -> c.getLevel() == 1 && c.getStatus() == CategoryStatus.ENABLED).collect(Collectors.toMap(Category::getId, c -> true));
            categoryIds = categoryList.stream().filter(c -> c.getLevel() == 2 && parentCategoryEnabled.get(c.getParentId()) != null).map(Category::getId).toList();
        }

        if (CollectionUtil.isEmpty(categoryIds)){
            // 要么是查询的是一级分类，其下无二级分类；要么目前数据库一个正常的二级分类都没有。
            // 🔺防止后面查询由于传空列表反而把所有的分类都筛选查到了
            return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), List.of());
        }

        // 5. 获取包含所有标签的文章列表
        List<Long> tagArticleIds = List.of();
        if (!tagIds.isEmpty()) {
            // tagIds 已去重
            tagArticleIds = articleTagMapper.getArticleIdsByTagIds(tagIds, tagIds.size());

            if (CollectionUtil.isEmpty(tagArticleIds)){
                // 查询标签列表非空、包含这些标签的文章 id 列表却为空，则返回空结果即可
                // 🔺同上，防空列表查询反而得到全部文章
                return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), List.of());
            }
        }

        // 6. 清洗搜索关键词。空白关键词视为未搜索，不进入全文检索分支
        boolean hasKeyword = StringUtils.hasText(queryDTO.getKeyword());
        String keyword = hasKeyword ? queryDTO.getKeyword().trim() : "";

        // 7. 有关键词时交给 zhparser 全文检索并生成高亮片段；普通列表继续复用 MyBatis Plus
        Map<Long, PublicArticleSearchBO> searchResultMap;
        if (hasKeyword) {
            Page<PublicArticleSearchBO> searchPage = Page.of(page.getCurrent(), page.getSize());
            articleMapper.selectPublicArticleSearchPage(
                    searchPage,
                    keyword,
                    categoryIds,
                    tagArticleIds,
                    queryDTO.getIsTop(),
                    queryDTO.getSort() != PublicArticleSort.LATEST
            );

            page.setTotal(searchPage.getTotal());
            page.setRecords(new ArrayList<>(searchPage.getRecords()));
            searchResultMap = searchPage.getRecords().stream()
                    .collect(Collectors.toMap(Article::getId, record -> record));
        }
        else {
            lambdaQuery()
                    .in(!CollectionUtil.isEmpty(tagIds), Article::getId, tagArticleIds)
                    .in(Article::getCategoryId, categoryIds)
                    .eq(Article::getStatus, ArticleStatus.PUBLISHED)
                    .eq(queryDTO.getIsTop() != null, Article::getIsTop, queryDTO.getIsTop())
                    .orderByDesc(queryDTO.getSort() != PublicArticleSort.LATEST, Article::getIsTop)
                    .orderByDesc(Article::getPublishedAt)
                    .orderByDesc(Article::getId)
                    .page(page);
            searchResultMap = Map.of();
        }

        // 8. 封装
        List<Article> records = page.getRecords();
        if (CollectionUtil.isEmpty(records)){
            return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), List.of());
        }

        // 分页中所有文章 id 并集
        List<Long> articleIds = records.stream().map(Article::getId).toList();
        // 分页中所有文章的分类 id 并集
        List<Long> articleCategoryIds = records.stream().map(Article::getCategoryId).distinct().toList();
        // 分页中所有文章的标签 id 并集
        List<Long> articleTagIds = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>()
                        .select(ArticleTag::getTagId)
                        .in(ArticleTag::getArticleId, articleIds)
        ).stream().map(ArticleTag::getTagId).distinct().toList();

        // 分类和标签对象的 map
        Map<Long, ArticleCategoryVO> categoryVoMap = getCategoryVoMap(articleCategoryIds);
        Map<Long, ArticleTagVO> tagVoMap = getTagVoMap(articleTagIds);   // 禁用的标签已过滤

        // 每篇文章对应哪些 tagId 的 map
        List<ArticleTag> articleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>()
                        .in(ArticleTag::getArticleId, articleIds)
        );
        Map<Long, List<Long>> articleTagIdsMap = articleTags.stream()
                .collect(Collectors.groupingBy(
                        ArticleTag::getArticleId,
                        Collectors.mapping(ArticleTag::getTagId, Collectors.toList())
                ));

        List<PublicArticleListItemVO> res = new ArrayList<>();
        records.forEach(article -> {
            PublicArticleListItemVO vo = BeanUtil.copyProperties(article, PublicArticleListItemVO.class);

            PublicArticleSearchBO searchResult = searchResultMap.get(article.getId());
            if (searchResult != null) {
                vo.setHighlightedTitle(toSafeSearchHighlight(searchResult.getHighlightedTitle()));
                vo.setSearchSnippet(toSafeSearchHighlight(searchResult.getSearchSnippet()));
            }

            vo.setCategory(categoryVoMap.get(article.getCategoryId()));

            List<ArticleTagVO> articleTagVOList = new ArrayList<>();
            List<Long> list = articleTagIdsMap.get(article.getId());
            if (list != null){
                list.forEach(tagId -> {
                    ArticleTagVO articleTagVO = tagVoMap.get(tagId);
                    if (articleTagVO != null) articleTagVOList.add(articleTagVO);
                });
            }

            vo.setTags(articleTagVOList);

            res.add(vo);
        });

        // 9. 返回
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), res);
    }


    /**
     * 获取前台文章详情
     *
     * @param articleId
     * @return
     */
    @Override
    public PublicArticleDetailVO getPublicArticleDetail(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null){
            throw new BizException(ResultCode.ARTICLE_NOT_FOUND);
        }
        if (article.getStatus() != ArticleStatus.PUBLISHED){
            throw new BizException(ResultCode.ARTICLE_NOT_VISIBLE);
        }

        PublicArticleDetailVO vo = BeanUtil.copyProperties(article, PublicArticleDetailVO.class);

        // 1. 封装分类
        Category category = categoryMapper.selectById(article.getCategoryId());
        if (category == null){
            throw new BizException(ResultCode.ARTICLE_CATEGORY_NOT_FOUND);
        }
        if (category.getStatus() == CategoryStatus.DISABLED){
            throw new BizException(ResultCode.ARTICLE_CATEGORY_DISABLED);
        }
        Category parentCategory = categoryMapper.selectById(category.getParentId());
        if (parentCategory == null){
            throw new BizException(ResultCode.ARTICLE_CATEGORY_NOT_FOUND);
        }
        if (parentCategory.getStatus() == CategoryStatus.DISABLED){
            throw new BizException(ResultCode.ARTICLE_CATEGORY_DISABLED);
        }
        ArticleCategoryParentVO articleCategoryParentVO = BeanUtil.copyProperties(parentCategory, ArticleCategoryParentVO.class);
        ArticleCategoryVO articleCategoryVO = BeanUtil.copyProperties(category, ArticleCategoryVO.class);
        articleCategoryVO.setParent(articleCategoryParentVO);

        vo.setCategory(articleCategoryVO);

        // 2. 封装标签
        List<Long> tagIds = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>()
                        .eq(ArticleTag::getArticleId, article.getId())
        ).stream().map(ArticleTag::getTagId).toList();
        List<ArticleTagVO> articleTagVOList = new ArrayList<>();
        if (!CollectionUtil.isEmpty(tagIds)){
            List<Tag> tags = tagMapper.selectList(
                    new LambdaQueryWrapper<Tag>()
                            .in(Tag::getId, tagIds)
            );
            // 关联标签如果不存在（属于数据异常，不报错，过滤掉继续返回文章详情）
            /*if (tags == null || tags.size() != tagIds.size()){
                throw new BizException(ResultCode.ARTICLE_TAG_NOT_FOUND);
            }*/
            tags.stream().filter(tag -> tag.getStatus() == TagStatus.ENABLED).forEach(tag -> {
                // 标签失效通常不应该影响文章本身是否可读，不报错，过滤掉返回文章详情
                /*if (tag.getStatus() == TagStatus.DISABLED){
                    throw new BizException(ResultCode.ARTICLE_TAG_DISABLED);
                }*/
                articleTagVOList.add(BeanUtil.copyProperties(tag, ArticleTagVO.class));
            });
        }

        vo.setTags(articleTagVOList);

        // 3. 封装作者
        User author = userMapper.selectById(article.getAuthorId());
        if (author == null) {
            throw new BizException(ResultCode.ARTICLE_AUTHOR_NOT_FOUND);
        }

        vo.setAuthor(BeanUtil.copyProperties(author, ArticleAuthorVO.class));

        // 4. 返回
        return vo;
    }


    /**
     * 将 PostgreSQL ts_headline 生成的内部标记转换成可安全渲染的高亮 HTML。
     * 原始标题、摘要和正文会先整体转义，只有服务端约定的标记会转换成 mark 标签。
     *
     * @param headline 带内部命中标记的文本
     * @return 安全高亮 HTML；没有完整命中标记时返回 null
     */
    private String toSafeSearchHighlight(String headline) {
        if (!StringUtils.hasText(headline)
                || !headline.contains(SEARCH_HIGHLIGHT_START)
                || !headline.contains(SEARCH_HIGHLIGHT_END)) {
            return null;
        }

        return HtmlUtils.htmlEscape(headline)
                .replace(SEARCH_HIGHLIGHT_START, SEARCH_HIGHLIGHT_MARK)
                .replace(SEARCH_HIGHLIGHT_END, "</mark>");
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

        Category parentCategory = categoryMapper.selectById(category.getParentId());
        if (parentCategory == null) {
            throw new BizException(ResultCode.ARTICLE_CATEGORY_NOT_FOUND);
        }
        if (parentCategory.getStatus() == CategoryStatus.DISABLED){
            throw new BizException(ResultCode.ARTICLE_CATEGORY_DISABLED);
        }
    }


    /**
     * 创建和更新文章的共同操作：
     * 1. 校验分类合法性
     * 2. 判断是否需要填入发表时间
     * 3. 渲染文章内容
     *
     * @param oldArticle 为空则是创建文章，非空则是更新文章
     * @param articleUpsertRequestDTO
     * @return 基本填充完毕的文章对象
     */
    private Article upsertArticleCommonOperation(Article oldArticle, ArticleUpsertRequestDTO articleUpsertRequestDTO) {
        // 1. 校验分类合法性
        checkArticleCategory(articleUpsertRequestDTO.getCategoryId());

        // 2. 判断是否需要填入发表时间
        Article article = BeanUtil.copyProperties(articleUpsertRequestDTO, Article.class);
        if (articleUpsertRequestDTO.getStatus() == ArticleStatus.PUBLISHED && (oldArticle == null || oldArticle.getPublishedAt() == null)){
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


    /**
     * 校验更新的文章状态是否合法
     *
     * @param oldArticle
     * @param newStatus
     */
    private void checkUpdateArticleStatus(Article oldArticle, ArticleStatus newStatus) {
        if (oldArticle.getPublishedAt() == null) {
            if (newStatus == ArticleStatus.OFFLINE){
                // 草稿 只能转 已发布
                throw new BizException(ResultCode.ARTICLE_STATUS_TRANSITION_INVALID);
            }
        }
        else{
            if (newStatus == ArticleStatus.DRAFT){
                // 已发布 只能转 下线，下线 只能转 已发布
                throw new BizException(ResultCode.ARTICLE_STATUS_TRANSITION_INVALID);
            }
        }
    }


    /**
     * 根据分类 id 数组（已去重）获取封装好了的 Map<Long,ArticleCategoryVO>（包括封装 ArticleCategoryParentVO）
     *
     * @param categoryIds
     * @return
     */
    private Map<Long, ArticleCategoryVO> getCategoryVoMap(List<Long> categoryIds){
        if (CollectionUtil.isEmpty(categoryIds)){
            return new HashMap<>();
        }

        List<Category> categoryList = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .in(Category::getId, categoryIds)
        );

        // p.s. Objects::nonNull 其实不加也可以，因为 categoryId 是 Article 的必填字段，理论上就是全都是二级分类，不会有父分类 Id 为 null 的情况；加上增强健壮性。
        // 注意 distinct() 对象是 Long id 而不是 Category 对象、filter 的对象也是 Long id 而不是 Category 对象
        // 即注意 distinct() 和 filter() 在流中的位置
        List<Long> parentCategoryIds = categoryList.stream().map(Category::getParentId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ArticleCategoryParentVO> parentCategoryVoMap = parentCategoryIds.isEmpty()
                ? Map.of()
                : BeanUtil.copyToList(
                        categoryMapper.selectList(
                                new LambdaQueryWrapper<Category>()
                                        .in(Category::getId, parentCategoryIds)
                        ), ArticleCategoryParentVO.class)
                .stream().collect(Collectors.toMap(ArticleCategoryParentVO::getId, category -> category));

        List<ArticleCategoryVO> categoryVoList = new ArrayList<>();
        categoryList.forEach(category -> {
            ArticleCategoryVO vo = BeanUtil.copyProperties(category, ArticleCategoryVO.class);
            vo.setParent(parentCategoryVoMap.get(category.getParentId()));
            categoryVoList.add(vo);
        });
        Map<Long, ArticleCategoryVO> categoryVoMap = categoryVoList.stream().collect(Collectors.toMap(ArticleCategoryVO::getId, category -> category));

        return categoryVoMap;
    }


    /**
     * 根据标签 id 数组（已去重）获取封装好了的 Map<Long,ArticleCategoryVO>
     *     标签已禁用时过滤，不封装进返回对象。
     *
     * @param tagIds
     * @return
     */
    private Map<Long, ArticleTagVO> getTagVoMap(List<Long> tagIds) {
        if (CollectionUtil.isEmpty(tagIds)){
            return new HashMap<>();
        }

        List<Tag> tags = tagMapper.selectByIds(tagIds);
        return tags.stream().filter(t -> t.getStatus() == TagStatus.ENABLED).collect(Collectors.toMap(Tag::getId, tag -> BeanUtil.copyProperties(tag, ArticleTagVO.class)));
    }
}
