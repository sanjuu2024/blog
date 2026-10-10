package com.ccsanjuu.blog.modules.article;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.article.model.dto.PublicArticleQueryDTO;
import com.ccsanjuu.blog.modules.article.model.vo.PublicArticleListItemVO;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.dto.ArticleUpsertRequestDTO;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.tag.model.entity.Tag;
import com.ccsanjuu.blog.modules.tag.model.enums.TagStatus;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.api.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ArticleSearchIntegrationTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Test
    void disabledAssociationsShouldAllowAdminSaveWhileKeepingPublicVisibilityRules() {
        Long articleId = jdbcTemplate.queryForObject("select id from blog_article where status = 'PUBLISHED' order by id limit 1", Long.class);
        Article existing = articleMapper.selectById(articleId);
        Category child = categoryMapper.selectById(existing.getCategoryId());
        Long tagId = jdbcTemplate.queryForObject("select id from blog_tag order by id limit 1", Long.class);
        categoryMapper.updateById(Category.builder().id(child.getId()).status(CategoryStatus.DISABLED).build());
        categoryMapper.updateById(Category.builder().id(child.getParentId()).status(CategoryStatus.DISABLED).build());
        tagMapper.updateById(Tag.builder().id(tagId).status(TagStatus.DISABLED).build());
        ArticleUpsertRequestDTO request = ArticleUpsertRequestDTO.builder()
                .title("禁用关联测试").contentMd("正文内容").categoryId(child.getId())
                .tagIds(java.util.List.of(tagId)).status(ArticleStatus.PUBLISHED).build();

        var created = articleService.createArticle(existing.getAuthorId(), request);
        articleService.updateArticle(created.getId(), existing.getAuthorId(), request);
        assertEquals(java.util.List.of(tagId), articleService.getArticleDetail(created.getId()).getTagIds());
        assertEquals(ResultCode.ARTICLE_CATEGORY_DISABLED, assertThrows(BizException.class,
                () -> articleService.getPublicArticleDetail(created.getId(), null, null)).getResultCode());

        categoryMapper.updateById(Category.builder().id(child.getId()).status(CategoryStatus.ENABLED).build());
        categoryMapper.updateById(Category.builder().id(child.getParentId()).status(CategoryStatus.ENABLED).build());
        assertTrue(articleService.getPublicArticleDetail(created.getId(), null, null).getTags().isEmpty());
    }

    @Test
    void publicListAndSearchShouldReturnTheSameStoredCommentCount() {
        String title = "Spring Boot 双 Token 登录实践";
        Long articleId = jdbcTemplate.queryForObject("select id from blog_article where title = ?", Long.class, title);
        articleMapper.updateById(Article.builder().id(articleId).commentCount(7).build());
        PublicArticleQueryDTO listQuery = new PublicArticleQueryDTO();
        listQuery.setPageSize(100);

        var listedArticle = articleService.getPublicArticleList(listQuery).getRecords().stream()
                .filter(item -> articleId.equals(item.getId())).findFirst().orElseThrow();

        assertEquals(7, listedArticle.getCommentCount());
        assertEquals(7, assertSingleTitle("Spring Boot", title).getCommentCount());
    }

    @Test
    void publicArticleSearchShouldMatchAllConfiguredFieldsAndHideUnpublishedArticles() {
        PublicArticleListItemVO titleMatch = assertSingleTitle(
                "sPrInG bOoT",
                "Spring Boot 双 Token 登录实践"
        );
        assertTrue(titleMatch.getHighlightedTitle().contains("<mark"));

        PublicArticleListItemVO summaryMatch = assertSingleTitle(
                "职责划分",
                "Spring Boot 双 Token 登录实践"
        );
        assertTrue(summaryMatch.getSearchSnippet().contains("<mark"));

        PublicArticleListItemVO contentMatch = assertSingleTitle(
                "访问体验和安全边界",
                "Spring Boot 双 Token 登录实践"
        );
        assertTrue(contentMatch.getSearchSnippet().contains("<mark"));

        PublicArticleQueryDTO query = new PublicArticleQueryDTO();
        query.setKeyword("后台页面优先保证");

        PageResult<PublicArticleListItemVO> result = articleService.getPublicArticleList(query);

        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void publicArticleSearchShouldCombineCategoryAndTagFilters() {
        Long javaCategoryId = jdbcTemplate.queryForObject(
                "select id from blog_category where name = 'Java'",
                Long.class
        );
        Long algorithmCategoryId = jdbcTemplate.queryForObject(
                "select id from blog_category where name = '算法'",
                Long.class
        );
        Long solutionTagId = jdbcTemplate.queryForObject(
                "select id from blog_tag where name = '题解'",
                Long.class
        );

        PublicArticleQueryDTO matchingQuery = new PublicArticleQueryDTO();
        matchingQuery.setKeyword("Spring Boot");
        matchingQuery.setCategoryId(javaCategoryId);
        matchingQuery.setTagIds(java.util.List.of(solutionTagId));

        PageResult<PublicArticleListItemVO> matchingResult = articleService.getPublicArticleList(matchingQuery);
        assertEquals(1, matchingResult.getTotal());

        matchingQuery.setCategoryId(algorithmCategoryId);
        PageResult<PublicArticleListItemVO> excludedResult = articleService.getPublicArticleList(matchingQuery);
        assertTrue(excludedResult.getRecords().isEmpty());
    }

    @Test
    void publicArticleSearchShouldMatchSingleChineseCharacter() {
        Long articleId = jdbcTemplate.queryForObject(
                "select id from blog_article where status = 'PUBLISHED' limit 1",
                Long.class
        );
        jdbcTemplate.update(
                "update blog_article set title = ?, summary = ?, content_text = ? where id = ?",
                "阿斯顿夫",
                "单字搜索测试",
                "正文内容",
                articleId
        );

        PublicArticleQueryDTO query = new PublicArticleQueryDTO();
        query.setKeyword("阿");

        PageResult<PublicArticleListItemVO> result = articleService.getPublicArticleList(query);

        assertEquals(1, result.getTotal());
        assertEquals("阿斯顿夫", result.getRecords().getFirst().getTitle());
    }

    @Test
    void publicArticleSearchShouldMatchNumericSubstring() {
        Long articleId = jdbcTemplate.queryForObject(
                "select id from blog_article where status = 'PUBLISHED' limit 1",
                Long.class
        );
        jdbcTemplate.update(
                "update blog_article set title = ?, summary = ?, content_text = ? where id = ?",
                "111",
                "数字搜索测试",
                "正文内容",
                articleId
        );

        assertEquals("111", assertSingleTitle("1", "111").getTitle());
        assertEquals("111", assertSingleTitle("11", "111").getTitle());
    }

    @Test
    void publicArticleSearchShouldSortByRelevanceBeforePublishTime() {
        var articleIds = jdbcTemplate.queryForList(
                "select id from blog_article where status = 'PUBLISHED' order by id limit 2",
                Long.class
        );
        assertEquals(2, articleIds.size());

        jdbcTemplate.update(
                "update blog_article set title = ?, summary = ?, content_text = ? where id = ?",
                "相关度标题 ranktest",
                "普通摘要",
                "普通正文",
                articleIds.get(0)
        );
        jdbcTemplate.update(
                "update blog_article set title = ?, summary = ?, content_text = ? where id = ?",
                "普通标题",
                "普通摘要",
                "相关度正文 ranktest",
                articleIds.get(1)
        );

        PublicArticleQueryDTO query = new PublicArticleQueryDTO();
        query.setKeyword("ranktest");

        PageResult<PublicArticleListItemVO> result = articleService.getPublicArticleList(query);

        assertEquals(2, result.getTotal());
        assertEquals(articleIds.get(0), result.getRecords().getFirst().getId());
    }

    // 分别用标题、摘要和正文中的关键词调用真实数据库，确认三个搜索字段都能返回同一篇已发布文章
    private PublicArticleListItemVO assertSingleTitle(String keyword, String expectedTitle) {
        PublicArticleQueryDTO query = new PublicArticleQueryDTO();
        query.setKeyword(keyword);

        PageResult<PublicArticleListItemVO> result = articleService.getPublicArticleList(query);

        assertEquals(1, result.getTotal());
        assertEquals(expectedTitle, result.getRecords().getFirst().getTitle());
        return result.getRecords().getFirst();
    }
}
