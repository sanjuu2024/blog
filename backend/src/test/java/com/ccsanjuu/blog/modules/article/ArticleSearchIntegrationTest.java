package com.ccsanjuu.blog.modules.article;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.article.model.dto.PublicArticleQueryDTO;
import com.ccsanjuu.blog.modules.article.model.vo.PublicArticleListItemVO;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ArticleSearchIntegrationTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
