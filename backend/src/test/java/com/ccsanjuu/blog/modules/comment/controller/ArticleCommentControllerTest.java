package com.ccsanjuu.blog.modules.comment.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.config.SecurityConfig;
import com.ccsanjuu.blog.config.WebMvcConfig;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.mapper.ArticleTagMapper;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.comment.model.dto.CreateCommentRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.PublicCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentMutationVO;
import com.ccsanjuu.blog.modules.comment.model.vo.PublicCommentItemVO;
import com.ccsanjuu.blog.modules.comment.service.CommentService;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ArticleCommentController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        WebMvcConfig.class,
        ArticleCommentControllerTest.SecurityTestConfig.class
})
class ArticleCommentControllerTest {

    private static final Long USER_ID = 10002L;
    private static final Long ARTICLE_ID = 40001L;
    private static final Long COMMENT_ID = 50001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey signingKey;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private TokenVersionService tokenVersionService;

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
    private MessageMapper messageMapper;

    @MockitoBean
    private TagMapper tagMapper;

    @MockitoBean
    private UserMapper userMapper;

    @BeforeEach
    void setUpTokenVersion() {
        when(tokenVersionService.getCurrentVersion(anyLong())).thenReturn(0L);
    }

    @Test
    void publicCommentListShouldAllowGuest() throws Exception {
        when(commentService.getPublicCommentList(eq(ARTICLE_ID), eq(null), any(PublicCommentQueryDTO.class)))
                .thenReturn(PageResult.of(0, 1, 10, List.of()));

        mockMvc.perform(get("/api/v1/articles/{articleId}/comments", ARTICLE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        verify(commentService).getPublicCommentList(eq(ARTICLE_ID), eq(null), any(PublicCommentQueryDTO.class));
    }

    @Test
    void publicCommentListShouldPassCurrentUserWhenTokenExists() throws Exception {
        when(commentService.getPublicCommentList(eq(ARTICLE_ID), eq(USER_ID), any(PublicCommentQueryDTO.class)))
                .thenReturn(PageResult.of(1, 1, 10, List.of(PublicCommentItemVO.builder()
                        .id(COMMENT_ID)
                        .articleId(ARTICLE_ID)
                        .status(CommentStatus.PENDING)
                        .isMine(true)
                        .build())));

        mockMvc.perform(get("/api/v1/articles/{articleId}/comments", ARTICLE_ID)
                        .header("Authorization", "Bearer " + accessToken(USER_ID, UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].isMine").value(true));

        verify(commentService).getPublicCommentList(eq(ARTICLE_ID), eq(USER_ID), any(PublicCommentQueryDTO.class));
    }

    @Test
    void publicCommentListShouldRejectInvalidTokenInsteadOfFallingBackToGuest() throws Exception {
        mockMvc.perform(get("/api/v1/articles/{articleId}/comments", ARTICLE_ID)
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(101001));

        verifyNoInteractions(commentService);
    }

    @Test
    void createCommentShouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/articles/{articleId}/comments", ARTICLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"这篇文章不错\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(101001));
    }

    @Test
    void createCommentShouldPassCurrentUserAndRequestBody() throws Exception {
        when(commentService.createComment(eq(ARTICLE_ID), eq(USER_ID), any(CreateCommentRequestDTO.class)))
                .thenReturn(CommentMutationVO.builder()
                        .id(COMMENT_ID)
                        .articleId(ARTICLE_ID)
                        .status(CommentStatus.PENDING)
                        .build());

        mockMvc.perform(post("/api/v1/articles/{articleId}/comments", ARTICLE_ID)
                        .header("Authorization", "Bearer " + accessToken(USER_ID, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"这篇文章不错\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(COMMENT_ID));

        ArgumentCaptor<CreateCommentRequestDTO> captor = ArgumentCaptor.forClass(CreateCommentRequestDTO.class);
        verify(commentService).createComment(eq(ARTICLE_ID), eq(USER_ID), captor.capture());
        assertEquals("这篇文章不错", captor.getValue().getContent());
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
