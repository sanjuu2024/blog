package com.ccsanjuu.blog.modules.article;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ccsanjuu.blog.modules.article.support.ArticleContentRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticlePreviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ArticleContentRenderer renderer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void previewShouldMatchSavedRenderingWithoutChangingArticlesAuditOrViews() throws Exception {
        Long articleCount = jdbcTemplate.queryForObject("select count(*) from blog_article", Long.class);
        Long auditCount = jdbcTemplate.queryForObject("select count(*) from blog_admin_audit_log", Long.class);
        Long views = jdbcTemplate.queryForObject("select coalesce(sum(view_count), 0) from blog_article", Long.class);
        String markdown = """
                # 未保存标题

                \\(a*b=2^{14}\\)

                ![替代文本](https://img.example.com/example.png "图注")

                ```java
                int a = 1;
                ```

                <script>alert(1)</script><img src="https://img.example.com/plain.png" onerror="alert(1)">
                """;
        String response = mockMvc.perform(post("/api/v1/admin/articles/preview")
                        .with(user("preview-admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("contentMd", markdown))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
        String html = objectMapper.readTree(response).path("data").path("contentHtml").asText();

        assertEquals(renderer.convertMarkdownToHtml(markdown), html);
        assertTrue(html.contains("\\(a*b=2^{14}\\)"));
        assertTrue(html.contains("title=\"图注\""));
        assertTrue(html.contains("language-java"));
        assertFalse(html.contains("<script"));
        assertFalse(html.contains("onerror"));
        assertEquals(articleCount, jdbcTemplate.queryForObject("select count(*) from blog_article", Long.class));
        assertEquals(auditCount, jdbcTemplate.queryForObject("select count(*) from blog_admin_audit_log", Long.class));
        assertEquals(views, jdbcTemplate.queryForObject("select coalesce(sum(view_count), 0) from blog_article", Long.class));
    }

    @Test
    void previewShouldRequireAdminAndValidateOnlyTheMarkdownField() throws Exception {
        mockMvc.perform(post("/api/v1/admin/articles/preview")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contentMd\":\"# Test\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/admin/articles/preview").with(user("reader").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contentMd\":\"# Test\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/articles/preview").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contentMd\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.contentHtml").value(""));
        mockMvc.perform(post("/api/v1/admin/articles/preview").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contentMd\":null}"))
                .andExpect(status().isBadRequest());
    }
}
