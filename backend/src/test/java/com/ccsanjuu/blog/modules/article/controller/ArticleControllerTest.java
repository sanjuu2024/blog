package com.ccsanjuu.blog.modules.article.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.config.WebMvcConfig;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.mapper.ArticleTagMapper;
import com.ccsanjuu.blog.modules.article.model.dto.PublicArticleQueryDTO;
import com.ccsanjuu.blog.modules.article.model.enums.PublicArticleSort;
import com.ccsanjuu.blog.modules.article.model.vo.PublicArticleDetailVO;
import com.ccsanjuu.blog.modules.article.model.vo.PublicArticleListItemVO;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ArticleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        WebMvcConfig.class
})
class ArticleControllerTest {

    private static final Long ARTICLE_ID = 40001L;
    private static final Long CATEGORY_ID = 21001L;
    private static final Long TAG_ID = 30001L;
    private static final Long SECOND_TAG_ID = 30002L;

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
    private TagMapper tagMapper;

    @MockitoBean
    private ArticleMapper articleMapper;

    @MockitoBean
    private ArticleTagMapper articleTagMapper;

    @Test
    void getPublicArticleListShouldBindQueryAndCallService() throws Exception {
        when(articleService.getPublicArticleList(any(PublicArticleQueryDTO.class)))
                .thenReturn(PageResult.of(1, 1, 10, List.of(PublicArticleListItemVO.builder()
                        .id(ARTICLE_ID)
                        .title("Spring Boot notes")
                        .publishedAt(OffsetDateTime.parse("2026-05-20T10:00:00+08:00"))
                        .build())));

        mockMvc.perform(get("/api/v1/articles")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("keyword", "Spring Boot")
                        .param("categoryId", CATEGORY_ID.toString())
                        .param("tagIds", TAG_ID.toString(), SECOND_TAG_ID.toString())
                        .param("isTop", "true")
                        .param("sort", "LATEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].id").value(ARTICLE_ID))
                .andExpect(jsonPath("$.data.records[0].highlightedTitle").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].searchSnippet").doesNotExist());

        ArgumentCaptor<PublicArticleQueryDTO> queryCaptor =
                ArgumentCaptor.forClass(PublicArticleQueryDTO.class);
        verify(articleService).getPublicArticleList(queryCaptor.capture());
        assertEquals(1, queryCaptor.getValue().getPageNum());
        assertEquals(10, queryCaptor.getValue().getPageSize());
        assertEquals("Spring Boot", queryCaptor.getValue().getKeyword());
        assertEquals(CATEGORY_ID, queryCaptor.getValue().getCategoryId());
        assertEquals(List.of(TAG_ID, SECOND_TAG_ID), queryCaptor.getValue().getTagIds());
        assertEquals(true, queryCaptor.getValue().getIsTop());
        assertEquals(PublicArticleSort.LATEST, queryCaptor.getValue().getSort());
    }

    @Test
    void getPublicArticleListShouldRejectInvalidQuery() throws Exception {
        mockMvc.perform(get("/api/v1/articles")
                        .param("pageNum", "0")
                        .param("pageSize", "21")
                        .param("keyword", "a".repeat(101))
                        .param("categoryId", "0")
                        .param("tagIds", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001));
    }

    @Test
    void getPublicArticleListShouldRejectInvalidSort() throws Exception {
        mockMvc.perform(get("/api/v1/articles")
                        .param("sort", "POPULAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001));
    }

    @Test
    void getPublicArticleDetailShouldCallService() throws Exception {
        when(articleService.getPublicArticleDetail(eq(ARTICLE_ID)))
                .thenReturn(PublicArticleDetailVO.builder()
                        .id(ARTICLE_ID)
                        .title("Spring Boot notes")
                        .build());

        mockMvc.perform(get("/api/v1/articles/{articleId}", ARTICLE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(ARTICLE_ID));

        verify(articleService).getPublicArticleDetail(eq(ARTICLE_ID));
    }
}
