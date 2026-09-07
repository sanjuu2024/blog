package com.ccsanjuu.blog.modules.article.controller;

import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.config.WebMvcConfig;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.mapper.ArticleTagMapper;
import com.ccsanjuu.blog.modules.article.model.dto.ArticleUpsertRequestDTO;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.article.model.vo.CreatedArticleVO;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminArticleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        WebMvcConfig.class
})
class AdminArticleControllerTest {

    private static final Long USER_ID = 10001L;
    private static final Long ARTICLE_ID = 40001L;
    private static final Long CATEGORY_ID = 21001L;
    private static final Long TAG_ID = 30001L;
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-20T10:00:00+08:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

    @MockitoBean
    private AuthMapper authMapper;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private CategoryMapper categoryMapper;

    @MockitoBean
    private CommentMapper commentMapper;

    @MockitoBean
    private MessageMapper messageMapper;

    @MockitoBean
    private TagMapper tagMapper;

    @MockitoBean
    private ArticleMapper articleMapper;

    @MockitoBean
    private ArticleTagMapper articleTagMapper;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getArticleDetailShouldRejectInvalidArticleId() throws Exception {
        mockMvc.perform(get("/api/v1/admin/articles/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001))
                .andExpect(jsonPath("$.data[0].field").value("articleId"));
    }

    @Test
    void createArticleShouldRejectInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "contentMd": "",
                                  "categoryId": 0,
                                  "status": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001))
                .andExpect(jsonPath("$.data[0].field").exists());
    }

    @Test
    void createArticleShouldPassPrincipalAndRequestToService() throws Exception {
        when(articleService.createArticle(eq(USER_ID), any(ArticleUpsertRequestDTO.class)))
                .thenReturn(CreatedArticleVO.builder()
                        .id(ARTICLE_ID)
                        .status(ArticleStatus.DRAFT)
                        .createdAt(CREATED_AT)
                .build());

        SecurityContextHolder.getContext().setAuthentication(adminAuthentication());
        mockMvc.perform(post("/api/v1/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Spring Boot notes",
                                  "summary": "A short summary",
                                  "contentMd": "# Hello",
                                  "categoryId": 21001,
                                  "tagIds": [30001],
                                  "coverUrl": "https://example.com/cover.png",
                                  "isTop": false,
                                  "status": "DRAFT",
                                  "allowComment": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(ARTICLE_ID))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        ArgumentCaptor<ArticleUpsertRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(ArticleUpsertRequestDTO.class);
        verify(articleService).createArticle(eq(USER_ID), requestCaptor.capture());
        assertEquals("Spring Boot notes", requestCaptor.getValue().getTitle());
        assertEquals(CATEGORY_ID, requestCaptor.getValue().getCategoryId());
        assertEquals(List.of(TAG_ID), requestCaptor.getValue().getTagIds());
        assertEquals(ArticleStatus.DRAFT, requestCaptor.getValue().getStatus());
    }

    private Authentication adminAuthentication() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "admin", "ADMIN", "ACTIVE");
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}
