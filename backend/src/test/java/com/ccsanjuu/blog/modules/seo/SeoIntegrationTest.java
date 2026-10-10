package com.ccsanjuu.blog.modules.seo;

import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"blog.site-url=https://blog.example.com", "blog.seo.html-template=classpath:seo/index.html"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class SeoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private UserMapper userMapper;

    private Article article;
    private User author;
    private Category parent;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        author = User.builder().username("seo_" + suffix).nickname("SEO作者")
                .email(suffix + "@example.com").passwordHash("test-hash").role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE).tokenVersion(0L).emailVerified(true).avatarUrl("").build();
        userMapper.insert(author);
        parent = Category.builder().name("SEO_" + suffix).level(1).status(CategoryStatus.ENABLED).sortNo(0).build();
        categoryMapper.insert(parent);
        Category child = Category.builder().name("文章_" + suffix).parentId(parent.getId()).level(2)
                .status(CategoryStatus.ENABLED).sortNo(0).build();
        categoryMapper.insert(child);
        article = Article.builder().title("SEO中文测试").summary("文章摘要").contentMd("正文内容")
                .contentHtml("<p>正文内容</p>").contentText("正文内容").coverUrl("")
                .categoryId(child.getId()).authorId(author.getId()).status(ArticleStatus.PUBLISHED)
                .isTop(false).allowComment(true).viewCount(7).likeCount(0).commentCount(0)
                .publishedAt(OffsetDateTime.now(ZoneOffset.UTC)).build();
        articleMapper.insert(article);
    }

    @Test
    void htmlMetadataAndSitemapShouldExposeMetadataWithoutBodyOrCountingViews() throws Exception {
        String path = "/articles/" + article.getId();
        mockMvc.perform(get(path).param("replyId", "51"))
                .andExpect(status().isOk()).andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(not(containsString("正文内容"))))
                .andExpect(content().string(not(containsString("<article>"))))
                .andExpect(content().string(containsString("https://blog.example.com" + path)))
                .andExpect(content().string(not(containsString("replyId"))));
        mockMvc.perform(get("/api/v1/seo").param("path", path))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.type").value("article"))
                .andExpect(jsonPath("$.data.canonicalUrl").value("https://blog.example.com" + path));
        var metadata = articleService.getPublicArticleDetail(article.getId(), null, null).getSeo();
        assertEquals("https://blog.example.com" + path, metadata.getCanonicalUrl());
        assertEquals("SEO中文测试 - 青禾边", metadata.getTitle());
        mockMvc.perform(get("/sitemap.xml")).andExpect(status().isOk())
                .andExpect(content().string(containsString("https://blog.example.com" + path + "</loc>")));
        assertEquals(7, articleMapper.selectById(article.getId()).getViewCount());
        mockMvc.perform(get("/api/v1/articles/" + article.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seo.canonicalUrl").value("https://blog.example.com" + path))
                .andExpect(jsonPath("$.data.seo.title").value(metadata.getTitle()));

        articleMapper.updateById(Article.builder().id(article.getId()).status(ArticleStatus.OFFLINE).build());
        mockMvc.perform(get(path)).andExpect(status().isNotFound());
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(content().string(not(containsString("https://blog.example.com" + path + "</loc>"))));
    }

    @Test
    void categoryVisibilityAndDeletedAuthorShouldFollowExistingArticleRules() throws Exception {
        String path = "/articles/" + article.getId();
        userMapper.updateById(User.builder().id(author.getId()).deletedAt(OffsetDateTime.now(ZoneOffset.UTC)).build());
        mockMvc.perform(get(path)).andExpect(status().isOk());
        categoryMapper.updateById(Category.builder().id(parent.getId()).status(CategoryStatus.DISABLED).build());
        mockMvc.perform(get(path)).andExpect(status().isNotFound());
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(content().string(not(containsString("https://blog.example.com" + path + "</loc>"))));
    }

    @Test
    void robotsAndInvalidPagesShouldNotExposePrivateMetadata() throws Exception {
        mockMvc.perform(get("/robots.txt")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Disallow: /admin")))
                .andExpect(content().string(containsString("Disallow: /api/\n")))
                .andExpect(content().string(containsString("Allow: /api/v1/articles\n")))
                .andExpect(content().string(containsString("Allow: /api/v1/users/*/public-profile\n")))
                .andExpect(content().string(not(containsString("Allow: /api/v1/users/me"))));
        mockMvc.perform(get("/api/v1/seo").param("path", "/admin"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/articles/not-an-id")).andExpect(status().isNotFound());
    }
}
