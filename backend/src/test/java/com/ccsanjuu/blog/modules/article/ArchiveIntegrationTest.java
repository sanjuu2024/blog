package com.ccsanjuu.blog.modules.article;

import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ArchiveIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    private Category parent;
    private Category child;
    private Long tagId;
    private Article older;
    private Article newer;

    @BeforeEach
    void setUp() {
        String suffix = Long.toString(System.nanoTime(), 36);
        Long authorId = jdbcTemplate.queryForObject("select id from blog_user order by id limit 1", Long.class);
        parent = Category.builder()
                .name("归档" + suffix)
                .level(1)
                .status(CategoryStatus.ENABLED)
                .sortNo(0)
                .build();
        categoryMapper.insert(parent);
        child = Category.builder()
                .name("子类" + suffix)
                .parentId(parent.getId())
                .level(2)
                .status(CategoryStatus.ENABLED)
                .sortNo(0)
                .build();
        categoryMapper.insert(child);
        tagId = jdbcTemplate.queryForObject("insert into blog_tag(name) values (?) returning id", Long.class, "archive" + suffix);
        older = insert("置顶旧文章", "2096-12-31T16:00:00Z", true, authorId, ArticleStatus.PUBLISHED);
        newer = insert("最新文章", "2097-02-01T00:00:00Z", false, authorId, ArticleStatus.PUBLISHED);
        insert("上一年边界", "2096-12-31T15:59:59Z", false, authorId, ArticleStatus.PUBLISHED);
        insert("不可见草稿", "2097-03-01T00:00:00Z", false, authorId, ArticleStatus.DRAFT);
        insert("作者缺失", "2097-03-01T00:00:00Z", false, -1L, ArticleStatus.PUBLISHED);
        jdbcTemplate.update("insert into blog_article_tag(article_id, tag_id) values (?, ?)", newer.getId(), tagId);
    }

    @Test
    void yearTotalsAndPagesShareVisibilityAndShanghaiBoundaries() throws Exception {
        mockMvc.perform(get("/api/v1/archives/years").param("timeZone", "Asia/Shanghai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.year == 2097)].articleCount", contains(2)));
        mockMvc.perform(get("/api/v1/archives/articles")
                        .param("timeZone", "Asia/Shanghai")
                        .param("year", "2097")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.records[0].id").value(newer.getId()))
                .andExpect(jsonPath("$.data.records[0].category.parent.id").value(parent.getId()))
                .andExpect(jsonPath("$.data.records[0].category.id").value(child.getId()))
                .andExpect(jsonPath("$.data.records[0].contentHtml").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].tags").doesNotExist());
        mockMvc.perform(get("/api/v1/archives/articles")
                        .param("timeZone", "Asia/Shanghai")
                        .param("year", "2097")
                        .param("pageSize", "1")
                        .param("pageNum", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(older.getId()));

        assertEquals(0, articleMapper.selectById(newer.getId()).getViewCount());
    }

    @Test
    void categoryAndTagFiltersRespectParentVisibilityWithoutDuplicateRows() throws Exception {
        for (Long categoryId : new Long[] {parent.getId(), child.getId()}) {
            mockMvc.perform(get("/api/v1/archives/articles")
                            .param("timeZone", "Asia/Shanghai")
                            .param("categoryId", categoryId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(3));
        }
        mockMvc.perform(get("/api/v1/archives/articles")
                        .param("timeZone", "Asia/Shanghai")
                        .param("tagId", tagId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        categoryMapper.updateById(Category.builder().id(parent.getId()).status(CategoryStatus.DISABLED).build());
        mockMvc.perform(get("/api/v1/archives/years").param("timeZone", "Asia/Shanghai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.year == 2097)]").isEmpty());
        mockMvc.perform(get("/api/v1/archives/articles").param("timeZone", "Asia/Shanghai").param("year", "2097"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/v1/archives/articles")
                        .param("timeZone", "Asia/Shanghai")
                        .param("categoryId", child.getId().toString()))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/archives/articles")
                        .param("timeZone", "Asia/Shanghai")
                        .param("tagId", "9223372036854775807"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({"Asia/Shanghai,2", "UTC,1", "America/Los_Angeles,2"})
    void yearTotalsAndPagesAgreeForEachBrowserTimeZone(String timeZone, int expectedCount) throws Exception {
        insert("洛杉矶年末", "2098-01-01T07:59:59Z", false, newer.getAuthorId(), ArticleStatus.PUBLISHED);
        insert("洛杉矶下一年年初", "2098-01-01T08:00:00Z", false, newer.getAuthorId(), ArticleStatus.PUBLISHED);

        mockMvc.perform(get("/api/v1/archives/years").param("timeZone", timeZone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.year == 2097)].articleCount", contains(expectedCount)));
        mockMvc.perform(get("/api/v1/archives/articles").param("timeZone", timeZone).param("year", "2097"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(expectedCount))
                .andExpect(jsonPath("$.data.records.length()").value(expectedCount));
    }

    /**
     * 创建用于校验发布时间边界、置顶排序及公开可见性的测试文章。
     *
     * @param title 文章标题
     * @param publishedAt 首次发布时间
     * @param top 是否置顶
     * @param userId 作者 ID
     * @param status 文章状态
     * @return 已写入并回填 ID 的文章
     */
    private Article insert(String title, String publishedAt, boolean top, Long userId, ArticleStatus status) {
        Article article = Article.builder()
                .title(title)
                .contentMd("正文")
                .contentHtml("<p>正文</p>")
                .contentText("正文")
                .categoryId(child.getId())
                .authorId(userId)
                .status(status)
                .isTop(top)
                .viewCount(0)
                .publishedAt(OffsetDateTime.parse(publishedAt))
                .build();
        articleMapper.insert(article);
        return article;
    }
}
