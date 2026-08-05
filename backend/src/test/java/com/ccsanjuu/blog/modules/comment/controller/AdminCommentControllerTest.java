package com.ccsanjuu.blog.modules.comment.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.config.SecurityConfig;
import com.ccsanjuu.blog.config.WebMvcConfig;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.mapper.ArticleTagMapper;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.comment.model.dto.AdminCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentModerationRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentType;
import com.ccsanjuu.blog.modules.comment.model.vo.AdminCommentItemVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentMutationVO;
import com.ccsanjuu.blog.modules.comment.service.CommentService;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminCommentController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        WebMvcConfig.class,
        AdminCommentControllerTest.SecurityTestConfig.class
})
class AdminCommentControllerTest {

    private static final Long ADMIN_ID = 10001L;
    private static final Long USER_ID = 10002L;
    private static final Long ARTICLE_ID = 40001L;
    private static final Long COMMENT_ID = 50001L;
    private static final OffsetDateTime CREATED_AT_FROM = OffsetDateTime.parse("2026-05-01T00:00:00+08:00");
    private static final OffsetDateTime CREATED_AT_TO = OffsetDateTime.parse("2026-05-31T23:59:59+08:00");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey signingKey;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private ArticleMapper articleMapper;

    @MockitoBean
    private ArticleTagMapper articleTagMapper;

    @MockitoBean
    private AuthMapper authMapper;

    @MockitoBean
    private CategoryMapper categoryMapper;

    @MockitoBean
    private CommentMapper commentMapper;

    @MockitoBean
    private TagMapper tagMapper;

    @MockitoBean
    private UserMapper userMapper;

    @Test
    void adminCommentListShouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/comments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(101001));
    }

    @Test
    void adminCommentListShouldBindFiltersForAdmin() throws Exception {
        when(commentService.getAdminCommentList(any(AdminCommentQueryDTO.class)))
                .thenReturn(PageResult.of(1L, 1L, 10L, List.of(AdminCommentItemVO.builder()
                        .id(COMMENT_ID)
                        .articleId(ARTICLE_ID)
                        .status(CommentStatus.PENDING)
                        .type(CommentType.TOP_LEVEL)
                        .build())));

        mockMvc.perform(get("/api/v1/admin/comments")
                        .header("Authorization", "Bearer " + accessToken(ADMIN_ID, UserRole.ADMIN))
                        .queryParam("pageNum", "1")
                        .queryParam("pageSize", "10")
                        .queryParam("articleId", ARTICLE_ID.toString())
                        .queryParam("userId", USER_ID.toString())
                        .queryParam("status", "PENDING")
                        .queryParam("type", "TOP_LEVEL")
                        .queryParam("createdAtFrom", CREATED_AT_FROM.toString())
                        .queryParam("createdAtTo", CREATED_AT_TO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(COMMENT_ID));

        ArgumentCaptor<AdminCommentQueryDTO> captor = ArgumentCaptor.forClass(AdminCommentQueryDTO.class);
        verify(commentService).getAdminCommentList(captor.capture());
        assertEquals(ARTICLE_ID, captor.getValue().getArticleId());
        assertEquals(USER_ID, captor.getValue().getUserId());
        assertEquals(CommentStatus.PENDING, captor.getValue().getStatus());
        assertEquals(CommentType.TOP_LEVEL, captor.getValue().getType());
        assertEquals(CREATED_AT_FROM, captor.getValue().getCreatedAtFrom());
        assertEquals(CREATED_AT_TO, captor.getValue().getCreatedAtTo());
    }

    @Test
    void moderateCommentShouldPassAdminAndCommentIds() throws Exception {
        when(commentService.moderateComment(eq(COMMENT_ID), eq(ADMIN_ID), any(CommentModerationRequestDTO.class)))
                .thenReturn(CommentMutationVO.builder()
                        .id(COMMENT_ID)
                        .status(CommentStatus.APPROVED)
                        .build());

        mockMvc.perform(patch("/api/v1/admin/comments/{commentId}/moderation", COMMENT_ID)
                        .header("Authorization", "Bearer " + accessToken(ADMIN_ID, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(COMMENT_ID))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(commentService).moderateComment(eq(COMMENT_ID), eq(ADMIN_ID), any(CommentModerationRequestDTO.class));
    }

    @Test
    void moderateCommentShouldRejectInvalidCommentId() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/comments/{commentId}/moderation", -1)
                        .header("Authorization", "Bearer " + accessToken(ADMIN_ID, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001));
    }

    private String accessToken(Long userId, UserRole role) {
        return JwtUtil.generateAccessToken(
                signingKey,
                "sanjuu-blog",
                Duration.ofMinutes(15),
                userId,
                "tester",
                role.getValue(),
                UserStatus.ACTIVE.getValue()
        );
    }

    @TestConfiguration
    static class SecurityTestConfig {

        @Bean
        SecretKey jwtSigningKey() {
            return JwtUtil.createHmacShaKey("0123456789abcdef0123456789abcdef");
        }
    }
}
