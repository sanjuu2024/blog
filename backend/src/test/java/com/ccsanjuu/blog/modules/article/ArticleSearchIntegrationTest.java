package com.ccsanjuu.blog.modules.article;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.article.model.dto.PublicArticleQueryDTO;
import com.ccsanjuu.blog.modules.article.model.vo.PublicArticleListItemVO;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.dto.ArticleUpsertRequestDTO;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.article.model.enums.PublicArticleSort;
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
import org.springframework.web.util.HtmlUtils;

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
    void savingArticleShouldPreserveStrikethroughAndRegeneratePreviouslyCleanedHtml() {
        Long articleId = jdbcTemplate.queryForObject("select id from blog_article where status = 'PUBLISHED' order by id limit 1", Long.class);
        Article existing = articleMapper.selectById(articleId);
        String markdown = "普通文字 ~~已删除的 **重点** 内容~~ 后文";
        ArticleUpsertRequestDTO request = ArticleUpsertRequestDTO.builder()
                .title("删除线渲染回归").contentMd(markdown).categoryId(existing.getCategoryId())
                .tagIds(java.util.List.of()).status(ArticleStatus.PUBLISHED).build();

        var created = articleService.createArticle(existing.getAuthorId(), request);
        Article saved = articleMapper.selectById(created.getId());
        assertEquals(markdown, saved.getContentMd());
        assertTrue(saved.getContentHtml().contains("<del>已删除的 <strong>重点</strong> 内容</del>"));
        assertEquals("普通文字 已删除的 重点 内容 后文", saved.getContentText());
        assertEquals(saved.getContentHtml(), articleService.getPublicArticleDetail(created.getId(), null, null).getContentHtml());

        articleMapper.updateById(Article.builder().id(created.getId())
                .contentHtml("<p>普通文字 已删除的 <strong>重点</strong> 内容 后文</p>").build());
        articleService.updateArticle(created.getId(), existing.getAuthorId(), request);

        assertEquals(saved.getContentHtml(), articleMapper.selectById(created.getId()).getContentHtml());
        assertEquals(saved.getContentHtml(), articleService.getPublicArticleDetail(created.getId(), null, null).getContentHtml());
    }

    @Test
    void savingArticleShouldPreserveSmallBigAndMarkInPublicHtml() {
        Long articleId = jdbcTemplate.queryForObject("select id from blog_article where status = 'PUBLISHED' order by id limit 1", Long.class);
        Article existing = articleMapper.selectById(articleId);
        String markdown = "<small>小字</small> <big>大字</big> <mark>重点</mark>";
        ArticleUpsertRequestDTO request = ArticleUpsertRequestDTO.builder()
                .title("行内语义标签回归").contentMd(markdown).categoryId(existing.getCategoryId())
                .tagIds(java.util.List.of()).status(ArticleStatus.PUBLISHED).build();

        var created = articleService.createArticle(existing.getAuthorId(), request);
        Article saved = articleMapper.selectById(created.getId());

        assertTrue(saved.getContentHtml().contains("<small>小字</small>"));
        assertTrue(saved.getContentHtml().contains("<big>大字</big>"));
        assertTrue(saved.getContentHtml().contains("<mark>重点</mark>"));
        assertEquals(saved.getContentHtml(), articleService.getPublicArticleDetail(created.getId(), null, null).getContentHtml());
    }

    @Test
    void savingArticleShouldPreserveFontColorInBoldUnderlinedPublicHtml() {
        Long articleId = jdbcTemplate.queryForObject("select id from blog_article where status = 'PUBLISHED' order by id limit 1", Long.class);
        Article existing = articleMapper.selectById(articleId);
        String markdown = "**<u>③ <font color='red'>xxx观点是xxx首要的观点</font>。</u>**";
        ArticleUpsertRequestDTO request = ArticleUpsertRequestDTO.builder()
                .title("颜色标注回归").contentMd(markdown).categoryId(existing.getCategoryId())
                .tagIds(java.util.List.of()).status(ArticleStatus.PUBLISHED).build();

        var created = articleService.createArticle(existing.getAuthorId(), request);
        Article saved = articleMapper.selectById(created.getId());
        assertEquals(markdown, saved.getContentMd());
        assertTrue(saved.getContentHtml().contains("<strong><u>③ <font color=\"red\">xxx观点是xxx首要的观点</font>。</u></strong>"));
        assertEquals(saved.getContentHtml(), articleService.getPublicArticleDetail(created.getId(), null, null).getContentHtml());

        articleMapper.updateById(Article.builder().id(created.getId())
                .contentHtml("<strong><u>③ xxx观点是xxx首要的观点。</u></strong>").build());
        articleService.updateArticle(created.getId(), existing.getAuthorId(), request);
        assertEquals(saved.getContentHtml(), articleService.getPublicArticleDetail(created.getId(), null, null).getContentHtml());
    }

    @Test
    void savingArticleShouldConvertDoubleEqualsToMarkInPublicHtml() {
        Long articleId = jdbcTemplate.queryForObject("select id from blog_article where status = 'PUBLISHED' order by id limit 1", Long.class);
        Article existing = articleMapper.selectById(articleId);
        String markdown = "正文 ==高亮 **重点**== 后文";
        ArticleUpsertRequestDTO request = ArticleUpsertRequestDTO.builder()
                .title("Markdown 高亮回归").contentMd(markdown).categoryId(existing.getCategoryId())
                .tagIds(java.util.List.of()).status(ArticleStatus.PUBLISHED).build();

        var created = articleService.createArticle(existing.getAuthorId(), request);
        Article saved = articleMapper.selectById(created.getId());

        assertEquals(markdown, saved.getContentMd());
        assertTrue(saved.getContentHtml().contains("<mark>高亮 <strong>重点</strong></mark>"));
        assertEquals("正文 高亮 重点 后文", saved.getContentText());
        assertEquals(saved.getContentHtml(), articleService.getPublicArticleDetail(created.getId(), null, null).getContentHtml());
    }

    @Test
    void savingArticleShouldRenderBoldMarkAndUploadedHtmlImages() {
        Long articleId = jdbcTemplate.queryForObject("select id from blog_article where status = 'PUBLISHED' order by id limit 1", Long.class);
        Article existing = articleMapper.selectById(articleId);
        String markdown = """
                ① **<mark>立大志，自信自励。</mark>**树立正确的人生志向。

                <img src="https://img.example.com/upload.png" alt="架构&amp;图&quot;.png" />
                """;
        ArticleUpsertRequestDTO request = ArticleUpsertRequestDTO.builder()
                .title("粗体高亮与 HTML 图片回归").contentMd(markdown).categoryId(existing.getCategoryId())
                .tagIds(java.util.List.of()).status(ArticleStatus.PUBLISHED).build();

        var created = articleService.createArticle(existing.getAuthorId(), request);
        Article saved = articleMapper.selectById(created.getId());
        assertEquals(markdown, saved.getContentMd());
        assertTrue(saved.getContentHtml().contains("<strong><mark>立大志，自信自励。</mark></strong>树立正确的人生志向。"));
        assertEquals("架构&图\".png", org.jsoup.Jsoup.parseBodyFragment(saved.getContentHtml()).selectFirst("img").attr("alt"));

        articleMapper.updateById(Article.builder().id(created.getId()).contentHtml("<p>旧 HTML</p>").build());
        articleService.updateArticle(created.getId(), existing.getAuthorId(), request);

        assertEquals(saved.getContentHtml(), articleService.getPublicArticleDetail(created.getId(), null, null).getContentHtml());
    }

    @Test
    void savingArticleShouldPreserveLatexSourceAndRegeneratePreviouslyBrokenHtml() {
        Long articleId = jdbcTemplate.queryForObject("select id from blog_article where status = 'PUBLISHED' order by id limit 1", Long.class);
        Article existing = articleMapper.selectById(articleId);
        String markdown = """
                \\(a*b=2^{14}\\)

                \\[c*d=2^{23}\\]

                ```cpp
                int a = 1;
                ```
                """;
        ArticleUpsertRequestDTO request = ArticleUpsertRequestDTO.builder()
                .title("公式渲染回归").contentMd(markdown).categoryId(existing.getCategoryId())
                .tagIds(java.util.List.of()).status(ArticleStatus.PUBLISHED).build();

        var created = articleService.createArticle(existing.getAuthorId(), request);
        Article saved = articleMapper.selectById(created.getId());
        assertEquals(markdown, saved.getContentMd());
        assertTrue(saved.getContentHtml().contains("\\(a*b=2^{14}\\)"));
        assertTrue(saved.getContentHtml().contains("\\[c*d=2^{23}\\]"));
        assertTrue(saved.getContentText().contains("a*b=2^{14}"));

        articleMapper.updateById(Article.builder().id(created.getId()).contentHtml("<p>(a*b=2^{14})</p>").build());
        articleService.updateArticle(created.getId(), existing.getAuthorId(), request);

        assertEquals(markdown, articleService.getArticleDetail(created.getId()).getContentMd());
        assertTrue(articleService.getPublicArticleDetail(created.getId(), null, null).getContentHtml().contains("\\(a*b=2^{14}\\)"));
        assertTrue(articleService.getPublicArticleDetail(created.getId(), null, null).getContentHtml().contains("\\[c*d=2^{23}\\]"));
    }

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

    @Test
    void titleSubstringShouldSortBeforeSummaryAndBodyMatchesUnlessLatestIsRequested() {
        var articleIds = jdbcTemplate.queryForList(
                "select id from blog_article where status = 'PUBLISHED' order by id limit 3", Long.class
        );
        assertEquals(3, articleIds.size());
        jdbcTemplate.update(
                "update blog_article set title = ?, summary = ?, content_text = ?, is_top = false, published_at = ?::timestamptz where id = ?",
                "Codeforces 练习", "普通摘要", "普通正文", "2025-01-01T00:00:00Z", articleIds.get(0)
        );
        jdbcTemplate.update(
                "update blog_article set title = ?, summary = ?, content_text = ?, is_top = true, published_at = ?::timestamptz where id = ?",
                "摘要命中文章", "code ".repeat(30), "普通正文", "2026-01-01T00:00:00Z", articleIds.get(1)
        );
        jdbcTemplate.update(
                "update blog_article set title = ?, summary = ?, content_text = ?, is_top = false, published_at = ?::timestamptz where id = ?",
                "正文命中文章", "普通摘要", "code ".repeat(50), "2026-02-01T00:00:00Z", articleIds.get(2)
        );
        PublicArticleQueryDTO query = new PublicArticleQueryDTO();
        query.setKeyword("CoDe");
        query.setPageSize(1);

        PageResult<PublicArticleListItemVO> firstPage = articleService.getPublicArticleList(query);
        assertEquals(3, firstPage.getTotal());
        assertEquals(1, firstPage.getRecords().size());
        assertEquals(articleIds.get(0), firstPage.getRecords().getFirst().getId());

        query.setPageSize(10);
        query.setSort(PublicArticleSort.LATEST);
        assertEquals(
                java.util.List.of(articleIds.get(2), articleIds.get(1), articleIds.get(0)),
                articleService.getPublicArticleList(query).getRecords().stream().map(PublicArticleListItemVO::getId).toList()
        );
    }

    @Test
    void titleFullTextMatchShouldSharePriorityWithTitleSubstringMatch() {
        var articleIds = jdbcTemplate.queryForList(
                "select id from blog_article where status = 'PUBLISHED' order by id limit 2", Long.class
        );
        assertEquals(2, articleIds.size());
        jdbcTemplate.update(
                "update blog_article set title = ?, summary = ?, content_text = ?, is_top = false, published_at = ?::timestamptz where id = ?",
                "titlealpha middle titlebeta", "普通摘要", "普通正文", "2025-01-01T00:00:00Z", articleIds.get(0)
        );
        jdbcTemplate.update(
                "update blog_article set title = ?, summary = ?, content_text = ?, is_top = true, published_at = ?::timestamptz where id = ?",
                "prefixtitlealpha titlebetasuffix", "普通摘要", "普通正文", "2026-01-01T00:00:00Z", articleIds.get(1)
        );
        PublicArticleQueryDTO query = new PublicArticleQueryDTO();
        query.setKeyword("titlealpha titlebeta");

        PageResult<PublicArticleListItemVO> result = articleService.getPublicArticleList(query);

        assertEquals(2, result.getTotal());
        assertEquals(articleIds, result.getRecords().stream().map(PublicArticleListItemVO::getId).toList());
    }

    @Test
    void publicArticleSearchShouldMatchEnglishSubstringsInAllFields() {
        Long articleId = jdbcTemplate.queryForObject(
                "select id from blog_article where title = 'Spring Boot 双 Token 登录实践'", Long.class
        );
        Long categoryId = articleMapper.selectById(articleId).getCategoryId();
        jdbcTemplate.update(
                "update blog_article set title = ?, summary = ?, content_text = ? where id = ?",
                "AtCoder 练习", "在 codeforces 上练习", "code 正文命中", articleId
        );

        assertEquals(
                "<mark class=\"article-search-highlight\">At</mark>Coder 练习",
                assertSingleTitle("  aT  ", "AtCoder 练习", categoryId).getHighlightedTitle()
        );
        assertEquals(
                "在 <mark class=\"article-search-highlight\">code</mark>forces 上练习",
                assertSingleTitle("CODE", "AtCoder 练习", categoryId).getSearchSnippet()
        );
        assertEquals(
                "在 code<mark class=\"article-search-highlight\">forces</mark> 上练习",
                assertSingleTitle("forces", "AtCoder 练习", categoryId).getSearchSnippet()
        );

        // 通过 Mapper 更新以清理同一事务中的 MyBatis 一级缓存，确保重复搜索读取新样本。
        articleMapper.updateById(Article.builder()
                .id(articleId)
                .summary("普通摘要")
                .contentText("前".repeat(80) + "Codeforces" + "后".repeat(80))
                .build());

        assertEquals(
                "…" + "前".repeat(46) + "Code<mark class=\"article-search-highlight\">forces</mark>" + "后".repeat(50) + "…",
                HtmlUtils.htmlUnescape(assertSingleTitle("forces", "AtCoder 练习", categoryId).getSearchSnippet())
        );
    }

    @Test
    void literalBodySnippetsShouldRespectDocumentBoundaries() {
        Long articleId = jdbcTemplate.queryForObject(
                "select id from blog_article where title = 'Spring Boot 双 Token 登录实践'", Long.class
        );
        Long categoryId = articleMapper.selectById(articleId).getCategoryId();
        var bodies = java.util.List.of("Codeforces" + "后".repeat(80), "前".repeat(80) + "Codeforces", "Codeforces");
        String highlighted = "<mark class=\"article-search-highlight\">Code</mark>forces";
        var snippets = java.util.List.of(
                highlighted + "后".repeat(44) + "…",
                "…" + "前".repeat(50) + highlighted,
                highlighted
        );

        for (int i = 0; i < bodies.size(); i++) {
            articleMapper.updateById(Article.builder()
                    .id(articleId)
                    .summary("普通摘要")
                    .contentText(bodies.get(i))
                    .build());
            assertEquals(
                    snippets.get(i),
                    HtmlUtils.htmlUnescape(assertSingleTitle("code", "Spring Boot 双 Token 登录实践", categoryId).getSearchSnippet())
            );
        }
    }

    @Test
    void literalMatchesShouldBeIncludedAlongsideFullTextMatchesWithExistingSortAndPagination() {
        var articleIds = jdbcTemplate.queryForList(
                "select id from blog_article where status = 'PUBLISHED' order by id limit 2", Long.class
        );
        assertEquals(2, articleIds.size());
        jdbcTemplate.update(
                "update blog_article set title = ?, summary = ?, content_text = ?, is_top = false, published_at = ?::timestamptz where id = ?",
                "fragmentprobe", "普通摘要", "普通正文", "2025-01-01T00:00:00Z", articleIds.get(0)
        );
        jdbcTemplate.update(
                "update blog_article set title = ?, summary = ?, content_text = ?, is_top = true, published_at = ?::timestamptz where id = ?",
                "prefixfragmentprobesuffix", "普通摘要", "普通正文", "2026-01-01T00:00:00Z", articleIds.get(1)
        );
        PublicArticleQueryDTO query = new PublicArticleQueryDTO();
        query.setKeyword("fragmentprobe");
        query.setPageSize(1);

        PageResult<PublicArticleListItemVO> firstPage = articleService.getPublicArticleList(query);
        assertEquals(2, firstPage.getTotal());
        assertEquals(1, firstPage.getRecords().size());
        assertEquals(articleIds.get(0), firstPage.getRecords().getFirst().getId());

        query.setPageNum(2);
        PageResult<PublicArticleListItemVO> secondPage = articleService.getPublicArticleList(query);
        assertEquals(2, secondPage.getTotal());
        assertEquals(articleIds.get(1), secondPage.getRecords().getFirst().getId());
        assertEquals(
                "prefix<mark class=\"article-search-highlight\">fragmentprobe</mark>suffix",
                secondPage.getRecords().getFirst().getHighlightedTitle()
        );

        query.setPageNum(1);
        query.setSort(PublicArticleSort.LATEST);
        jdbcTemplate.update("update blog_article set is_top = (id = ?) where id in (?, ?)",
                articleIds.get(0), articleIds.get(0), articleIds.get(1));
        assertEquals(articleIds.get(1), articleService.getPublicArticleList(query).getRecords().getFirst().getId());
    }

    @Test
    void literalSearchShouldTreatWildcardsAsTextAndEscapeHtml() {
        Long articleId = jdbcTemplate.queryForObject(
                "select id from blog_article where title = 'Spring Boot 双 Token 登录实践'", Long.class
        );
        jdbcTemplate.update(
                "update blog_article set title = ?, summary = ?, content_text = ? where id = ?",
                "<script>_%</script>", "普通摘要", "普通正文", articleId
        );

        assertEquals(
                "&lt;script&gt;<mark class=\"article-search-highlight\">_%</mark>&lt;/script&gt;",
                assertSingleTitle("_%", "<script>_%</script>").getHighlightedTitle()
        );
    }

    @Test
    void literalSearchShouldRespectFiltersAndPublicVisibility() {
        Long articleId = jdbcTemplate.queryForObject(
                "select id from blog_article where title = 'Spring Boot 双 Token 登录实践'", Long.class
        );
        Article article = articleMapper.selectById(articleId);
        Long otherCategoryId = jdbcTemplate.queryForObject("select id from blog_category where name = '算法'", Long.class);
        Long tagId = jdbcTemplate.queryForObject("select id from blog_tag where name = '题解'", Long.class);
        jdbcTemplate.update("update blog_article set title = ?, is_top = false where id = ?", "LiteralNeedleSuffix", articleId);
        PublicArticleQueryDTO query = new PublicArticleQueryDTO();
        query.setKeyword("literalneedle");
        query.setCategoryId(article.getCategoryId());
        query.setTagIds(java.util.List.of(tagId));
        assertEquals(1, articleService.getPublicArticleList(query).getTotal());

        query.setCategoryId(otherCategoryId);
        assertTrue(articleService.getPublicArticleList(query).getRecords().isEmpty());
        query.setCategoryId(null);
        query.setIsTop(true);
        assertTrue(articleService.getPublicArticleList(query).getRecords().isEmpty());
        query.setIsTop(null);

        for (String status : java.util.List.of("DRAFT", "OFFLINE")) {
            jdbcTemplate.update("update blog_article set status = ? where id = ?", status, articleId);
            assertTrue(articleService.getPublicArticleList(query).getRecords().isEmpty());
        }
        jdbcTemplate.update("update blog_article set status = 'PUBLISHED' where id = ?", articleId);
        Category category = categoryMapper.selectById(article.getCategoryId());
        categoryMapper.updateById(Category.builder().id(category.getId()).status(CategoryStatus.DISABLED).build());
        assertTrue(articleService.getPublicArticleList(query).getRecords().isEmpty());
        categoryMapper.updateById(Category.builder().id(category.getId()).status(CategoryStatus.ENABLED).build());
        categoryMapper.updateById(Category.builder().id(category.getParentId()).status(CategoryStatus.DISABLED).build());
        assertTrue(articleService.getPublicArticleList(query).getRecords().isEmpty());
    }

    /**
     * 在指定分类中验证唯一搜索结果，避免其他分类的演示文章影响子串匹配断言。
     *
     * @param keyword 搜索关键词
     * @param expectedTitle 预期文章标题
     * @param categoryId 分类 ID；为空时不限分类
     * @return 唯一命中的文章
     */
    private PublicArticleListItemVO assertSingleTitle(String keyword, String expectedTitle, Long categoryId) {
        PublicArticleQueryDTO query = new PublicArticleQueryDTO();
        query.setKeyword(keyword);
        query.setCategoryId(categoryId);

        PageResult<PublicArticleListItemVO> result = articleService.getPublicArticleList(query);

        assertEquals(1, result.getTotal());
        assertEquals(expectedTitle, result.getRecords().getFirst().getTitle());
        return result.getRecords().getFirst();
    }

    // 分别用标题、摘要和正文中的关键词调用真实数据库，确认三个搜索字段都能返回同一篇已发布文章
    private PublicArticleListItemVO assertSingleTitle(String keyword, String expectedTitle) {
        return assertSingleTitle(keyword, expectedTitle, null);
    }
}
