package com.ccsanjuu.blog.modules.article;

import com.ccsanjuu.blog.modules.about.mapper.AboutPageMapper;
import com.ccsanjuu.blog.modules.about.model.dto.UpdateAboutPageRequestDTO;
import com.ccsanjuu.blog.modules.about.service.AboutPageService;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.dto.ArticleUpsertRequestDTO;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RubyContentIntegrationTest {

    @Autowired
    private ArticleService articleService;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private AboutPageMapper aboutPageMapper;
    @Autowired
    private AboutPageService aboutPageService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private User admin;
    private Category category;

    @BeforeEach
    void setUp() {
        String suffix = Long.toString(System.nanoTime(), 36);
        admin = User.builder().username("ruby" + suffix).nickname("Ruby 管理员")
                .email("ruby" + suffix + "@example.com").passwordHash("test-password")
                .role(UserRole.ADMIN).status(UserStatus.ACTIVE).tokenVersion(0L)
                .avatarUrl("").bio("").emailVerified(true).build();
        userMapper.insert(admin);
        Category parent = Category.builder().name("Ruby " + suffix).level(1)
                .status(CategoryStatus.ENABLED).sortNo(100).build();
        categoryMapper.insert(parent);
        category = Category.builder().name("日语").parentId(parent.getId()).level(2)
                .status(CategoryStatus.ENABLED).sortNo(100).build();
        categoryMapper.insert(category);
    }

    @Test
    void articleSavePreviewPublicReadAndSearchShouldAgreeOnRuby() throws Exception {
        String markdown = "私は<ruby onclick='alert(1)'>日本語<rp>（</rp>"
                + "<rt style='color:red'>にほんご</rt><rp>）</rp></ruby>を学ぶ。";
        ArticleUpsertRequestDTO request = ArticleUpsertRequestDTO.builder()
                .title("Ruby 注音测试").summary("").contentMd(markdown)
                .categoryId(category.getId()).tagIds(List.of()).status(ArticleStatus.PUBLISHED)
                .coverUrl("").isTop(false).allowComment(true).build();
        Long articleId = articleService.createArticle(admin.getId(), request).getId();
        var saved = articleMapper.selectById(articleId);

        assertEquals(markdown, saved.getContentMd());
        assertEquals("私は日本語を学ぶ。", saved.getContentText());
        assertEquals("にほんご", Jsoup.parseBodyFragment(saved.getContentHtml()).selectFirst("ruby rt").text());
        assertFalse(saved.getContentHtml().contains("onclick"));
        assertFalse(saved.getContentHtml().contains("style="));

        String preview = mockMvc.perform(post("/api/v1/admin/articles/preview")
                        .with(user("ruby-admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("contentMd", markdown))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(saved.getContentHtml(), objectMapper.readTree(preview).path("data").path("contentHtml").asText());

        String detail = mockMvc.perform(get("/api/v1/articles/{articleId}", articleId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(saved.getContentHtml(), objectMapper.readTree(detail).path("data").path("contentHtml").asText());
        String search = mockMvc.perform(get("/api/v1/articles").param("keyword", "日本語を学ぶ"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var records = objectMapper.readTree(search).path("data").path("records");
        assertTrue(StreamSupport.stream(records.spliterator(), false)
                .anyMatch(record -> record.path("id").asLong() == articleId));

        request.setContentMd("<ruby>漢字<rt>かんじ</rt></ruby>を覚える。");
        articleService.updateArticle(articleId, admin.getId(), request);
        var updated = articleMapper.selectById(articleId);
        assertEquals("漢字を覚える。", updated.getContentText());
        assertEquals("かんじ", Jsoup.parseBodyFragment(updated.getContentHtml()).selectFirst("ruby rt").text());
    }

    @Test
    void aboutPageShouldPersistRubyAndExposeOnlyBaseTextInPlainContent() {
        aboutPageMapper.deleteById(1L);
        String markdown = "<ruby>私<rt>わたし</rt></ruby>のブログです。";
        aboutPageService.upsertAboutPage(admin.getId(), new UpdateAboutPageRequestDTO(markdown));
        var stored = aboutPageMapper.selectById(1L);
        var publicPage = aboutPageService.getPublicAboutPage();

        assertEquals(markdown, stored.getContentMd());
        assertEquals("私のブログです。", stored.getContentText());
        assertEquals(stored.getContentHtml(), publicPage.getContentHtml());
        assertEquals("わたし", Jsoup.parseBodyFragment(publicPage.getContentHtml()).selectFirst("ruby rt").text());
    }
}
