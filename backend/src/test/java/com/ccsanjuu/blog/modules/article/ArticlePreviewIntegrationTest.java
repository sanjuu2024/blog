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

                ==预览高亮 **重点**==

                ① **<mark>立大志，自信自励。</mark>**树立正确的人生志向。

                **<u>③ <font color='red' onclick='alert(1)'>xxx观点是xxx首要的观点</font>。</u>**

                <img src="https://img.example.com/upload.png" alt="架构&amp;图&quot;.png" />

                ~~已删除的 **重点** 内容~~

                <small onclick="alert(1)">小字</small> <big style="color:red">大字</big> <mark class="custom">标记</mark>

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
        assertTrue(html.contains("<mark>预览高亮 <strong>重点</strong></mark>"));
        assertTrue(html.contains("<strong><mark>立大志，自信自励。</mark></strong>树立正确的人生志向。"));
        assertTrue(html.contains("<strong><u>③ <font color=\"red\">xxx观点是xxx首要的观点</font>。</u></strong>"));
        var uploadedImage = org.jsoup.Jsoup.parseBodyFragment(html).selectFirst("img[src$=upload.png]");
        assertEquals("架构&图\".png", uploadedImage.attr("alt"));
        assertFalse(uploadedImage.hasAttr("title"));
        assertTrue(html.contains("<del>已删除的 <strong>重点</strong> 内容</del>"));
        assertTrue(html.contains("<small>小字</small>"));
        assertTrue(html.contains("<big>大字</big>"));
        assertTrue(html.contains("<mark>标记</mark>"));
        assertTrue(html.contains("\\(a*b=2^{14}\\)"));
        assertTrue(html.contains("title=\"图注\""));
        assertTrue(html.contains("language-java"));
        assertFalse(html.contains("<script"));
        assertFalse(html.contains("onclick"));
        assertFalse(html.contains("style="));
        assertFalse(html.contains("class=\"custom\""));
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
