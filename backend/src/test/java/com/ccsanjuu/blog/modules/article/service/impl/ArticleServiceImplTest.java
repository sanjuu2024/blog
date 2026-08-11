package com.ccsanjuu.blog.modules.article.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.ccsanjuu.blog.modules.article.model.vo.AdminArticleDetailVO;
import com.ccsanjuu.blog.modules.article.model.vo.AdminArticleListItemVO;
import com.ccsanjuu.blog.modules.article.model.vo.CreatedArticleVO;
import com.ccsanjuu.blog.modules.article.model.vo.PublicArticleDetailVO;
import com.ccsanjuu.blog.modules.article.model.vo.PublicArticleListItemVO;
import com.ccsanjuu.blog.modules.article.model.vo.UpdatedArticleStatusVO;
import com.ccsanjuu.blog.modules.article.model.vo.UpdatedArticleVO;
import com.ccsanjuu.blog.modules.article.support.ArticleContentRenderer;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.tag.model.entity.Tag;
import com.ccsanjuu.blog.modules.tag.model.enums.TagStatus;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class ArticleServiceImplTest {

    private static final Long ARTICLE_ID = 40001L;
    private static final Long USER_ID = 10001L;
    private static final Long OTHER_USER_ID = 10002L;
    private static final Long PARENT_CATEGORY_ID = 20001L;
    private static final Long CATEGORY_ID = 21001L;
    private static final Long TAG_ID = 30001L;
    private static final Long SECOND_TAG_ID = 30002L;
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-20T10:00:00+08:00");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-05-20T11:00:00+08:00");
    private static final OffsetDateTime PUBLISHED_AT = OffsetDateTime.parse("2026-05-20T10:30:00+08:00");
    private static final String CONTENT_MD = "# Hello\n\nThis is **markdown**.";
    private static final String CONTENT_HTML = "<h1>Hello</h1><p>This is <strong>markdown</strong>.</p>";
    private static final String CONTENT_TEXT = "Hello\n\nThis is markdown.";

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleTagMapper articleTagMapper;

    @Mock
    private ArticleContentRenderer articleContentRenderer;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private UserMapper userMapper;

    private ArticleServiceImpl articleService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        initTableInfo(Article.class);
        initTableInfo(ArticleTag.class);
        initTableInfo(Category.class);
        initTableInfo(Tag.class);
    }

    @BeforeEach
    void setUp() {
        articleService = new ArticleServiceImpl(
                articleTagMapper,
                articleContentRenderer,
                categoryMapper,
                articleMapper,
                tagMapper,
                userMapper
        );
        ReflectionTestUtils.setField(articleService, "baseMapper", articleMapper);
        ReflectionTestUtils.setField(articleService, "entityClass", Article.class);
        ReflectionTestUtils.setField(articleService, "mapperClass", ArticleMapper.class);
    }

    private static void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) != null) {
            return;
        }
        MapperBuilderAssistant mapperBuilderAssistant = new MapperBuilderAssistant(
                new MybatisConfiguration(),
                ""
        );
        mapperBuilderAssistant.setCurrentNamespace(entityClass.getName());
        TableInfoHelper.initTableInfo(mapperBuilderAssistant, entityClass);
    }

    @Test
    void getArticleListShouldAttachCategoryAndParentCategory() {
        AdminArticleQueryDTO query = new AdminArticleQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);

        Article article = existingArticle(ArticleStatus.PUBLISHED, PUBLISHED_AT);
        when(articleMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<Article> page = invocation.getArgument(0);
            page.setTotal(1);
            page.setRecords(List.of(article));
            return page;
        });
        when(categoryMapper.selectList(any())).thenReturn(
                List.of(enabledChildCategory()),
                List.of(enabledParentCategory())
        );

        PageResult<AdminArticleListItemVO> result = articleService.getArticleList(query);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(CATEGORY_ID, result.getRecords().getFirst().getCategory().getId());
        assertEquals("Java", result.getRecords().getFirst().getCategory().getName());
        assertEquals(PARENT_CATEGORY_ID, result.getRecords().getFirst().getCategory().getParent().getId());
        assertEquals("Tech", result.getRecords().getFirst().getCategory().getParent().getName());
    }

    @Test
    void getArticleListShouldReturnEmptyPageWhenLevelOneCategoryHasNoChildren() {
        AdminArticleQueryDTO query = new AdminArticleQueryDTO();
        query.setPageNum(2);
        query.setPageSize(10);
        query.setCategoryId(PARENT_CATEGORY_ID);
        when(categoryMapper.selectById(PARENT_CATEGORY_ID)).thenReturn(enabledParentCategory());
        when(categoryMapper.selectList(any())).thenReturn(List.of());

        PageResult<AdminArticleListItemVO> result = articleService.getArticleList(query);

        assertEquals(0, result.getTotal());
        assertEquals(2, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertTrue(result.getRecords().isEmpty());
        verify(articleMapper, never()).selectPage(any(Page.class), any());
    }

    @Test
    void getArticleDetailShouldReturnArticleWithTagIds() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(existingArticle(ArticleStatus.DRAFT, null));
        when(articleTagMapper.selectList(any())).thenReturn(List.of(
                ArticleTag.builder().articleId(ARTICLE_ID).tagId(TAG_ID).build(),
                ArticleTag.builder().articleId(ARTICLE_ID).tagId(SECOND_TAG_ID).build()
        ));

        AdminArticleDetailVO result = articleService.getArticleDetail(ARTICLE_ID);

        assertEquals(ARTICLE_ID, result.getId());
        assertEquals(List.of(TAG_ID, SECOND_TAG_ID), result.getTagIds());
    }

    @Test
    void getArticleDetailShouldRejectMissingArticle() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(null);

        BizException exception = assertThrows(BizException.class,
                () -> articleService.getArticleDetail(ARTICLE_ID));

        assertEquals(ResultCode.ARTICLE_NOT_FOUND, exception.getResultCode());
        verifyNoInteractions(articleTagMapper);
    }

    @Test
    void createArticleShouldRejectOfflineStatus() {
        ArticleUpsertRequestDTO request = validUpsertRequest(ArticleStatus.OFFLINE);

        BizException exception = assertThrows(BizException.class,
                () -> articleService.createArticle(USER_ID, request));

        assertEquals(ResultCode.ARTICLE_STATUS_TRANSITION_INVALID, exception.getResultCode());
        verifyNoInteractions(categoryMapper, articleMapper, articleTagMapper, tagMapper, articleContentRenderer);
    }

    @Test
    void createArticleShouldRejectMissingCategory() {
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(null);

        BizException exception = assertThrows(BizException.class,
                () -> articleService.createArticle(USER_ID, validUpsertRequest(ArticleStatus.DRAFT)));

        assertEquals(ResultCode.ARTICLE_CATEGORY_NOT_FOUND, exception.getResultCode());
        verify(articleMapper, never()).insert(any(Article.class));
    }

    @Test
    void createArticleShouldRejectLevelOneCategory() {
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(enabledParentCategory());

        BizException exception = assertThrows(BizException.class,
                () -> articleService.createArticle(USER_ID, validUpsertRequest(ArticleStatus.DRAFT)));

        assertEquals(ResultCode.ARTICLE_CATEGORY_LEVEL_INVALID, exception.getResultCode());
        verify(articleMapper, never()).insert(any(Article.class));
    }

    @Test
    void createArticleShouldRejectDisabledCategory() {
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(disabledChildCategory());

        BizException exception = assertThrows(BizException.class,
                () -> articleService.createArticle(USER_ID, validUpsertRequest(ArticleStatus.DRAFT)));

        assertEquals(ResultCode.ARTICLE_CATEGORY_DISABLED, exception.getResultCode());
        verify(articleMapper, never()).insert(any(Article.class));
    }

    @Test
    void createArticleShouldRenderContentInsertArticleAndDistinctTags() {
        AtomicReference<Article> insertedArticle = new AtomicReference<>();
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(enabledChildCategory());
        when(categoryMapper.selectById(PARENT_CATEGORY_ID)).thenReturn(enabledParentCategory());
        when(articleContentRenderer.convertMarkdownToHtml(CONTENT_MD)).thenReturn(CONTENT_HTML);
        when(articleContentRenderer.convertToText(CONTENT_MD)).thenReturn(CONTENT_TEXT);
        when(articleMapper.insert(any(Article.class))).thenAnswer(invocation -> {
            Article article = invocation.getArgument(0);
            article.setId(ARTICLE_ID);
            article.setCreatedAt(CREATED_AT);
            insertedArticle.set(article);
            return 1;
        });
        when(articleMapper.selectById(ARTICLE_ID)).thenAnswer(invocation -> insertedArticle.get());
        when(tagMapper.selectByIds(any())).thenReturn(List.of(
                enabledTag(TAG_ID),
                enabledTag(SECOND_TAG_ID)
        ));

        ArticleUpsertRequestDTO request = validUpsertRequest(ArticleStatus.PUBLISHED);
        request.setTagIds(List.of(TAG_ID, TAG_ID, SECOND_TAG_ID));
        CreatedArticleVO result = articleService.createArticle(USER_ID, request);

        ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);
        verify(articleMapper).insert(articleCaptor.capture());
        Article article = articleCaptor.getValue();
        assertEquals(USER_ID, article.getAuthorId());
        assertEquals(CATEGORY_ID, article.getCategoryId());
        assertEquals(CONTENT_HTML, article.getContentHtml());
        assertEquals(CONTENT_TEXT, article.getContentText());
        assertNotNull(article.getPublishedAt());
        assertEquals(ARTICLE_ID, result.getId());
        assertEquals(ArticleStatus.PUBLISHED, result.getStatus());
        assertEquals(CREATED_AT, result.getCreatedAt());
        verify(articleTagMapper).insertBatch(ARTICLE_ID, List.of(TAG_ID, SECOND_TAG_ID));
    }

    @Test
    void createArticleShouldRejectMissingTag() {
        mockSuccessfulArticleInsert();
        when(tagMapper.selectByIds(any())).thenReturn(List.of(enabledTag(TAG_ID)));

        ArticleUpsertRequestDTO request = validUpsertRequest(ArticleStatus.DRAFT);
        request.setTagIds(List.of(TAG_ID, SECOND_TAG_ID));
        BizException exception = assertThrows(BizException.class,
                () -> articleService.createArticle(USER_ID, request));

        assertEquals(ResultCode.ARTICLE_TAG_NOT_FOUND, exception.getResultCode());
        verify(articleTagMapper, never()).insertBatch(any(), any());
    }

    @Test
    void createArticleShouldRejectDisabledTag() {
        mockSuccessfulArticleInsert();
        when(tagMapper.selectByIds(any())).thenReturn(List.of(disabledTag(TAG_ID)));

        BizException exception = assertThrows(BizException.class,
                () -> articleService.createArticle(USER_ID, validUpsertRequest(ArticleStatus.DRAFT)));

        assertEquals(ResultCode.ARTICLE_TAG_DISABLED, exception.getResultCode());
        verify(articleTagMapper, never()).insertBatch(any(), any());
    }

    @Test
    void updateArticleShouldRejectMissingArticle() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(null);

        BizException exception = assertThrows(BizException.class,
                () -> articleService.updateArticle(ARTICLE_ID, USER_ID, validUpsertRequest(ArticleStatus.DRAFT)));

        assertEquals(ResultCode.ARTICLE_NOT_FOUND, exception.getResultCode());
        verify(articleMapper, never()).updateById(any(Article.class));
    }

    @Test
    void updateArticleShouldRejectNoPermission() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(existingArticleByAuthor(OTHER_USER_ID));

        BizException exception = assertThrows(BizException.class,
                () -> articleService.updateArticle(ARTICLE_ID, USER_ID, validUpsertRequest(ArticleStatus.DRAFT)));

        assertEquals(ResultCode.NO_PERMISSION, exception.getResultCode());
        verify(articleMapper, never()).updateById(any(Article.class));
    }

    @Test
    void updateDraftArticleShouldRejectOfflineStatus() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(existingArticle(ArticleStatus.DRAFT, null));

        BizException exception = assertThrows(BizException.class,
                () -> articleService.updateArticle(ARTICLE_ID, USER_ID, validUpsertRequest(ArticleStatus.OFFLINE)));

        assertEquals(ResultCode.ARTICLE_STATUS_TRANSITION_INVALID, exception.getResultCode());
        verify(articleMapper, never()).updateById(any(Article.class));
    }

    @Test
    void updatePublishedArticleShouldRejectDraftStatus() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(existingArticle(ArticleStatus.PUBLISHED, PUBLISHED_AT));

        BizException exception = assertThrows(BizException.class,
                () -> articleService.updateArticle(ARTICLE_ID, USER_ID, validUpsertRequest(ArticleStatus.DRAFT)));

        assertEquals(ResultCode.ARTICLE_STATUS_TRANSITION_INVALID, exception.getResultCode());
        verify(articleMapper, never()).updateById(any(Article.class));
    }

    @Test
    void updateDraftArticleShouldPublishAndReplaceTags() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(
                existingArticle(ArticleStatus.DRAFT, null),
                existingArticle(ArticleStatus.PUBLISHED, PUBLISHED_AT)
        );
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(enabledChildCategory());
        when(categoryMapper.selectById(PARENT_CATEGORY_ID)).thenReturn(enabledParentCategory());
        when(articleContentRenderer.convertMarkdownToHtml(CONTENT_MD)).thenReturn(CONTENT_HTML);
        when(articleContentRenderer.convertToText(CONTENT_MD)).thenReturn(CONTENT_TEXT);
        when(tagMapper.selectByIds(any())).thenReturn(List.of(enabledTag(TAG_ID)));

        UpdatedArticleVO result = articleService.updateArticle(
                ARTICLE_ID,
                USER_ID,
                validUpsertRequest(ArticleStatus.PUBLISHED)
        );

        ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);
        verify(articleMapper).updateById(articleCaptor.capture());
        assertEquals(ARTICLE_ID, articleCaptor.getValue().getId());
        assertEquals(CONTENT_HTML, articleCaptor.getValue().getContentHtml());
        assertEquals(CONTENT_TEXT, articleCaptor.getValue().getContentText());
        assertNotNull(articleCaptor.getValue().getPublishedAt());
        verify(articleTagMapper).delete(any());
        verify(articleTagMapper).insertBatch(ARTICLE_ID, List.of(TAG_ID));
        assertEquals(ARTICLE_ID, result.getId());
        assertEquals(ArticleStatus.PUBLISHED, result.getStatus());
        assertEquals(PUBLISHED_AT, result.getPublishedAt());
        assertEquals(UPDATED_AT, result.getUpdatedAt());
    }

    @Test
    void deleteArticleShouldRejectNoPermission() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(existingArticleByAuthor(OTHER_USER_ID));

        BizException exception = assertThrows(BizException.class,
                () -> articleService.deleteArticle(USER_ID, ARTICLE_ID));

        assertEquals(ResultCode.NO_PERMISSION, exception.getResultCode());
        verify(articleTagMapper, never()).delete(any());
        verify(articleMapper, never()).deleteById(eq(ARTICLE_ID));
    }

    @Test
    void deleteArticleShouldClearTagsAndDeleteArticle() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(existingArticle(ArticleStatus.DRAFT, null));

        articleService.deleteArticle(USER_ID, ARTICLE_ID);

        verify(articleTagMapper).delete(any());
        verify(articleMapper).deleteById(eq(ARTICLE_ID));
    }

    @Test
    void updateArticleStatusShouldRejectDraftToOffline() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(existingArticle(ArticleStatus.DRAFT, null));

        BizException exception = assertThrows(BizException.class,
                () -> articleService.updateArticleStatus(ARTICLE_ID, USER_ID,
                        updateStatusRequest(ArticleStatus.OFFLINE)));

        assertEquals(ResultCode.ARTICLE_STATUS_TRANSITION_INVALID, exception.getResultCode());
        verify(articleMapper, never()).update(isNull(), any());
    }

    @Test
    void updateArticleStatusShouldRejectPublishedToDraft() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(existingArticle(ArticleStatus.PUBLISHED, PUBLISHED_AT));

        BizException exception = assertThrows(BizException.class,
                () -> articleService.updateArticleStatus(ARTICLE_ID, USER_ID,
                        updateStatusRequest(ArticleStatus.DRAFT)));

        assertEquals(ResultCode.ARTICLE_STATUS_TRANSITION_INVALID, exception.getResultCode());
        verify(articleMapper, never()).update(isNull(), any());
    }

    @Test
    void updateArticleStatusShouldPublishNeverPublishedArticle() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(
                existingArticle(ArticleStatus.DRAFT, null),
                existingArticle(ArticleStatus.PUBLISHED, PUBLISHED_AT)
        );
        when(articleMapper.update(isNull(), any())).thenReturn(1);

        UpdatedArticleStatusVO result = articleService.updateArticleStatus(
                ARTICLE_ID,
                USER_ID,
                updateStatusRequest(ArticleStatus.PUBLISHED)
        );

        verify(articleMapper).update(isNull(), any());
        assertEquals(ARTICLE_ID, result.getId());
        assertEquals(ArticleStatus.PUBLISHED, result.getStatus());
        assertEquals(UPDATED_AT, result.getUpdatedAt());
    }

    @Test
    void getPublicArticleListShouldFilterByCategoryAndTagsThenHideDisabledResponseTags() {
        PublicArticleQueryDTO query = new PublicArticleQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setCategoryId(CATEGORY_ID);
        query.setTagIds(List.of(TAG_ID, TAG_ID, SECOND_TAG_ID));
        query.setIsTop(true);
        query.setSort(PublicArticleSort.LATEST);

        AtomicReference<Wrapper<Article>> queryWrapper = new AtomicReference<>();

        List<ArticleTag> articleTags = List.of(
                ArticleTag.builder().articleId(ARTICLE_ID).tagId(TAG_ID).build(),
                ArticleTag.builder().articleId(ARTICLE_ID).tagId(SECOND_TAG_ID).build()
        );
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(enabledChildCategory());
        when(categoryMapper.selectById(PARENT_CATEGORY_ID)).thenReturn(enabledParentCategory());
        when(tagMapper.selectList(any())).thenReturn(List.of(enabledTag(TAG_ID), enabledTag(SECOND_TAG_ID)));
        when(articleTagMapper.getArticleIdsByTagIds(List.of(TAG_ID, SECOND_TAG_ID), 2))
                .thenReturn(List.of(ARTICLE_ID));
        when(articleMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<Article> page = invocation.getArgument(0);
            queryWrapper.set(invocation.getArgument(1));
            page.setTotal(1);
            page.setRecords(List.of(existingArticle(ArticleStatus.PUBLISHED, PUBLISHED_AT)));
            return page;
        });
        when(articleTagMapper.selectList(any())).thenReturn(articleTags, articleTags);
        when(categoryMapper.selectList(any())).thenReturn(
                List.of(enabledChildCategory()),
                List.of(enabledParentCategory())
        );
        when(tagMapper.selectByIds(any())).thenReturn(List.of(enabledTag(TAG_ID), disabledTag(SECOND_TAG_ID)));

        PageResult<PublicArticleListItemVO> result = articleService.getPublicArticleList(query);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        PublicArticleListItemVO item = result.getRecords().getFirst();
        assertEquals(ARTICLE_ID, item.getId());
        assertEquals(CATEGORY_ID, item.getCategory().getId());
        assertEquals(PARENT_CATEGORY_ID, item.getCategory().getParent().getId());
        assertEquals(1, item.getTags().size());
        assertEquals(TAG_ID, item.getTags().getFirst().getId());
        String sqlSegment = queryWrapper.get().getSqlSegment();
        assertTrue(sqlSegment.contains("is_top"));
        assertTrue(sqlSegment.contains("published_at DESC"));
        assertTrue(sqlSegment.contains("id DESC"));
        assertFalse(sqlSegment.contains("is_top DESC"));
    }

    @Test
    void getPublicArticleListShouldPrioritizeTopArticlesByDefault() {
        PublicArticleQueryDTO query = new PublicArticleQueryDTO();
        AtomicReference<Wrapper<Article>> queryWrapper = new AtomicReference<>();

        when(categoryMapper.selectList(any())).thenReturn(List.of(enabledParentCategory(), enabledChildCategory()));
        when(articleMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<Article> page = invocation.getArgument(0);
            queryWrapper.set(invocation.getArgument(1));
            page.setTotal(0);
            page.setRecords(List.of());
            return page;
        });

        articleService.getPublicArticleList(query);

        String sqlSegment = queryWrapper.get().getSqlSegment();
        assertTrue(sqlSegment.contains("is_top DESC"));
        assertTrue(sqlSegment.contains("published_at DESC"));
        assertTrue(sqlSegment.contains("id DESC"));
    }

    @Test
    void getPublicArticleListShouldUseFullTextSearchMapper() {
        PublicArticleQueryDTO query = new PublicArticleQueryDTO();
        query.setKeyword("  ReDiS  ");

        when(categoryMapper.selectList(any())).thenReturn(List.of(enabledParentCategory(), enabledChildCategory()));
        when(articleMapper.selectPublicArticleSearchPage(any(Page.class), any(), any(), any(), any(), eq(true)))
                .thenAnswer(invocation -> {
            Page<PublicArticleSearchBO> page = invocation.getArgument(0);
            page.setTotal(0);
            page.setRecords(List.of());
            return page;
        });

        articleService.getPublicArticleList(query);

        verify(articleMapper).selectPublicArticleSearchPage(
                any(Page.class),
                eq("ReDiS"),
                eq(List.of(CATEGORY_ID)),
                eq(List.of()),
                isNull(),
                eq(true)
        );
        verify(articleMapper, never()).selectPage(any(Page.class), any());
    }

    @Test
    void getPublicArticleListShouldEscapeSearchHighlights() {
        PublicArticleQueryDTO query = new PublicArticleQueryDTO();
        query.setKeyword("Boot");

        PublicArticleSearchBO searchArticle = new PublicArticleSearchBO();
        searchArticle.setId(ARTICLE_ID);
        searchArticle.setTitle("Spring Boot notes");
        searchArticle.setSummary("A short summary");
        searchArticle.setCategoryId(CATEGORY_ID);
        searchArticle.setHighlightedTitle(
                "Spring __BLOG_SEARCH_HIGHLIGHT_START__Boot__BLOG_SEARCH_HIGHLIGHT_END__ notes"
        );
        searchArticle.setSearchSnippet(
                "<script>alert(1)</script> __BLOG_SEARCH_HIGHLIGHT_START__Boot__BLOG_SEARCH_HIGHLIGHT_END__"
        );

        when(categoryMapper.selectList(any())).thenReturn(
                List.of(enabledParentCategory(), enabledChildCategory()),
                List.of(enabledChildCategory()),
                List.of(enabledParentCategory())
        );
        when(articleMapper.selectPublicArticleSearchPage(any(Page.class), any(), any(), any(), any(), eq(true)))
                .thenAnswer(invocation -> {
            Page<PublicArticleSearchBO> page = invocation.getArgument(0);
            page.setTotal(1);
            page.setRecords(List.of(searchArticle));
            return page;
        });
        when(articleTagMapper.selectList(any())).thenReturn(List.of());

        PublicArticleListItemVO result = articleService.getPublicArticleList(query).getRecords().getFirst();

        assertEquals(
                "Spring <mark class=\"article-search-highlight\">Boot</mark> notes",
                result.getHighlightedTitle()
        );
        assertTrue(result.getSearchSnippet().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(result.getSearchSnippet().contains(
                "<mark class=\"article-search-highlight\">Boot</mark>"
        ));
        assertFalse(result.getSearchSnippet().contains("<script>"));
    }

    @Test
    void getPublicArticleListShouldRejectDisabledQueryTag() {
        PublicArticleQueryDTO query = new PublicArticleQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setTagIds(List.of(TAG_ID));
        when(tagMapper.selectList(any())).thenReturn(List.of(disabledTag(TAG_ID)));

        BizException exception = assertThrows(BizException.class,
                () -> articleService.getPublicArticleList(query));

        assertEquals(ResultCode.ARTICLE_TAG_DISABLED, exception.getResultCode());
        verify(articleMapper, never()).selectPage(any(Page.class), any());
    }

    @Test
    void getPublicArticleDetailShouldReturnPublishedArticleWithAuthorAndEnabledTags() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(existingArticle(ArticleStatus.PUBLISHED, PUBLISHED_AT));
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(enabledChildCategory());
        when(categoryMapper.selectById(PARENT_CATEGORY_ID)).thenReturn(enabledParentCategory());
        when(articleTagMapper.selectList(any())).thenReturn(List.of(
                ArticleTag.builder().articleId(ARTICLE_ID).tagId(TAG_ID).build(),
                ArticleTag.builder().articleId(ARTICLE_ID).tagId(SECOND_TAG_ID).build()
        ));
        when(tagMapper.selectList(any())).thenReturn(List.of(enabledTag(TAG_ID), disabledTag(SECOND_TAG_ID)));
        when(userMapper.selectById(USER_ID)).thenReturn(author());

        PublicArticleDetailVO result = articleService.getPublicArticleDetail(ARTICLE_ID);

        assertEquals(ARTICLE_ID, result.getId());
        assertEquals(CONTENT_HTML, result.getContentHtml());
        assertEquals(CATEGORY_ID, result.getCategory().getId());
        assertEquals(PARENT_CATEGORY_ID, result.getCategory().getParent().getId());
        assertEquals(1, result.getTags().size());
        assertEquals(TAG_ID, result.getTags().getFirst().getId());
        assertEquals(USER_ID, result.getAuthor().getId());
        assertEquals("ccsanjuu", result.getAuthor().getUsername());
    }

    @Test
    void getPublicArticleDetailShouldRejectMissingAuthor() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(existingArticle(ArticleStatus.PUBLISHED, PUBLISHED_AT));
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(enabledChildCategory());
        when(categoryMapper.selectById(PARENT_CATEGORY_ID)).thenReturn(enabledParentCategory());
        when(articleTagMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectById(USER_ID)).thenReturn(null);

        BizException exception = assertThrows(BizException.class,
                () -> articleService.getPublicArticleDetail(ARTICLE_ID));

        assertEquals(ResultCode.ARTICLE_AUTHOR_NOT_FOUND, exception.getResultCode());
        verify(userMapper).selectById(USER_ID);
    }

    @Test
    void getPublicArticleDetailShouldRejectNonPublishedArticle() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(existingArticle(ArticleStatus.DRAFT, null));

        BizException exception = assertThrows(BizException.class,
                () -> articleService.getPublicArticleDetail(ARTICLE_ID));

        assertEquals(ResultCode.ARTICLE_NOT_VISIBLE, exception.getResultCode());
        verifyNoInteractions(categoryMapper, articleTagMapper, tagMapper, userMapper);
    }

    @Test
    void getPublicArticleDetailShouldRejectDisabledParentCategory() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(existingArticle(ArticleStatus.PUBLISHED, PUBLISHED_AT));
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(enabledChildCategory());
        when(categoryMapper.selectById(PARENT_CATEGORY_ID)).thenReturn(disabledParentCategory());

        BizException exception = assertThrows(BizException.class,
                () -> articleService.getPublicArticleDetail(ARTICLE_ID));

        assertEquals(ResultCode.ARTICLE_CATEGORY_DISABLED, exception.getResultCode());
        verifyNoInteractions(articleTagMapper, tagMapper, userMapper);
    }

    private void mockSuccessfulArticleInsert() {
        AtomicReference<Article> insertedArticle = new AtomicReference<>();
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(enabledChildCategory());
        when(categoryMapper.selectById(PARENT_CATEGORY_ID)).thenReturn(enabledParentCategory());
        when(articleContentRenderer.convertMarkdownToHtml(CONTENT_MD)).thenReturn(CONTENT_HTML);
        when(articleContentRenderer.convertToText(CONTENT_MD)).thenReturn(CONTENT_TEXT);
        when(articleMapper.insert(any(Article.class))).thenAnswer(invocation -> {
            Article article = invocation.getArgument(0);
            article.setId(ARTICLE_ID);
            insertedArticle.set(article);
            return 1;
        });
        when(articleMapper.selectById(ARTICLE_ID)).thenAnswer(invocation -> insertedArticle.get());
    }

    private ArticleUpsertRequestDTO validUpsertRequest(ArticleStatus status) {
        return ArticleUpsertRequestDTO.builder()
                .title("Spring Boot notes")
                .summary("A short summary")
                .contentMd(CONTENT_MD)
                .categoryId(CATEGORY_ID)
                .tagIds(List.of(TAG_ID))
                .coverUrl("https://example.com/cover.png")
                .isTop(false)
                .status(status)
                .allowComment(true)
                .build();
    }

    private UpdateArticleStatusRequestDTO updateStatusRequest(ArticleStatus status) {
        return UpdateArticleStatusRequestDTO.builder()
                .status(status)
                .build();
    }

    private Article existingArticle(ArticleStatus status, OffsetDateTime publishedAt) {
        return Article.builder()
                .id(ARTICLE_ID)
                .title("Spring Boot notes")
                .summary("A short summary")
                .contentMd(CONTENT_MD)
                .contentHtml(CONTENT_HTML)
                .contentText(CONTENT_TEXT)
                .status(status)
                .categoryId(CATEGORY_ID)
                .authorId(USER_ID)
                .isTop(false)
                .allowComment(true)
                .publishedAt(publishedAt)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }

    private Article existingArticleByAuthor(Long authorId) {
        return Article.builder()
                .id(ARTICLE_ID)
                .title("Spring Boot notes")
                .summary("A short summary")
                .contentMd(CONTENT_MD)
                .contentHtml(CONTENT_HTML)
                .contentText(CONTENT_TEXT)
                .status(ArticleStatus.DRAFT)
                .categoryId(CATEGORY_ID)
                .authorId(authorId)
                .isTop(false)
                .allowComment(true)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }

    private Category enabledParentCategory() {
        return Category.builder()
                .id(PARENT_CATEGORY_ID)
                .name("Tech")
                .level(1)
                .status(CategoryStatus.ENABLED)
                .build();
    }

    private Category disabledParentCategory() {
        return Category.builder()
                .id(PARENT_CATEGORY_ID)
                .name("Tech")
                .level(1)
                .status(CategoryStatus.DISABLED)
                .build();
    }

    private Category enabledChildCategory() {
        return Category.builder()
                .id(CATEGORY_ID)
                .name("Java")
                .parentId(PARENT_CATEGORY_ID)
                .level(2)
                .status(CategoryStatus.ENABLED)
                .build();
    }

    private Category disabledChildCategory() {
        return Category.builder()
                .id(CATEGORY_ID)
                .name("Java")
                .parentId(PARENT_CATEGORY_ID)
                .level(2)
                .status(CategoryStatus.DISABLED)
                .build();
    }

    private Tag enabledTag(Long tagId) {
        return Tag.builder()
                .id(tagId)
                .name("tag-" + tagId)
                .status(TagStatus.ENABLED)
                .build();
    }

    private Tag disabledTag(Long tagId) {
        return Tag.builder()
                .id(tagId)
                .name("tag-" + tagId)
                .status(TagStatus.DISABLED)
                .build();
    }

    private User author() {
        return User.builder()
                .id(USER_ID)
                .username("ccsanjuu")
                .nickname("sanjuu")
                .avatarUrl("https://example.com/avatar.png")
                .bio("About me")
                .build();
    }
}
