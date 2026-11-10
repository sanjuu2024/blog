package com.ccsanjuu.blog.modules.article;

import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminArticleDetailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleService articleService;

    @Test
    void editorDetailShouldReturnLongMarkdownAndMetadataWithoutOtherBodyRepresentations() throws Exception {
        Long articleId = jdbcTemplate.queryForObject(
                "select id from blog_article where status = 'PUBLISHED' order by id limit 1", Long.class);
        String markdown = "# 长文章\n\n" + "用于编辑的长正文。".repeat(10000);
        articleMapper.updateById(Article.builder().id(articleId).contentMd(markdown).build());
        Article stored = articleMapper.selectById(articleId);
        var tagIds = articleService.getArticleDetail(articleId).getTagIds();

        String response = mockMvc.perform(get("/api/v1/admin/articles/{articleId}", articleId)
                        .with(user("editor").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(articleId))
                .andExpect(jsonPath("$.data.title").value(stored.getTitle()))
                .andExpect(jsonPath("$.data.contentMd").value(markdown))
                .andExpect(jsonPath("$.data.categoryId").value(stored.getCategoryId()))
                .andExpect(jsonPath("$.data.status").value(stored.getStatus().name()))
                .andExpect(jsonPath("$.data.isTop").value(stored.getIsTop()))
                .andExpect(jsonPath("$.data.allowComment").value(stored.getAllowComment()))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andReturn().getResponse().getContentAsString();
        var data = objectMapper.readTree(response).path("data");

        assertEquals(tagIds, objectMapper.readerForListOf(Long.class).readValue(data.path("tagIds")));
        assertFalse(data.has("contentHtml"));
        assertFalse(data.has("contentText"));
        assertEquals(stored.getContentHtml(), articleMapper.selectById(articleId).getContentHtml());
        assertEquals(stored.getContentText(), articleMapper.selectById(articleId).getContentText());
        assertEquals(stored.getContentHtml(), articleService.getPublicArticleDetail(articleId, null, null).getContentHtml());
    }

    @Test
    void editorDetailShouldKeepAdministratorPermissionAndMissingArticleResponse() throws Exception {
        mockMvc.perform(get("/api/v1/admin/articles/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/articles/1").with(user("reader").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/articles/{articleId}", Long.MAX_VALUE)
                        .with(user("editor").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(103001));
    }
}
