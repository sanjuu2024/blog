package com.ccsanjuu.blog.modules.comment.controller;

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
import com.ccsanjuu.blog.modules.comment.model.vo.CommentDeleteVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentReplyPageVO;
import com.ccsanjuu.blog.modules.comment.service.CommentService;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CommentController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        WebMvcConfig.class,
        CommentControllerTest.SecurityTestConfig.class
})
class CommentControllerTest {

    private static final Long USER_ID = 10002L;
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
    void replyListShouldBindCursorQuery() throws Exception {
        when(commentService.getRepliesByRootId(eq(COMMENT_ID), eq(null), any()))
                .thenReturn(CommentReplyPageVO.builder()
                        .records(List.of())
                        .nextCursor(null)
                        .hasNext(false)
                        .build());

        mockMvc.perform(get("/api/v1/comments/{commentId}/replies", COMMENT_ID)
                        .queryParam("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasNext").value(false));

        verify(commentService).getRepliesByRootId(eq(COMMENT_ID), eq(null), any());
    }

    @Test
    void replyListShouldPassCurrentUserWhenTokenExists() throws Exception {
        when(commentService.getRepliesByRootId(eq(COMMENT_ID), eq(USER_ID), any()))
                .thenReturn(CommentReplyPageVO.builder()
                        .records(List.of())
                        .nextCursor(null)
                        .hasNext(false)
                        .build());

        mockMvc.perform(get("/api/v1/comments/{commentId}/replies", COMMENT_ID)
                        .header("Authorization", "Bearer " + accessToken(USER_ID, UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasNext").value(false));

        verify(commentService).getRepliesByRootId(eq(COMMENT_ID), eq(USER_ID), any());
    }

    @Test
    void replyListShouldRejectInvalidTokenInsteadOfFallingBackToGuest() throws Exception {
        mockMvc.perform(get("/api/v1/comments/{commentId}/replies", COMMENT_ID)
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(101001));

        verifyNoInteractions(commentService);
    }

    @Test
    void deleteOwnCommentShouldRequireLogin() throws Exception {
        mockMvc.perform(delete("/api/v1/comments/{commentId}", COMMENT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(101001));
    }

    @Test
    void deleteOwnCommentShouldPassCurrentUser() throws Exception {
        when(commentService.deleteOwnComment(COMMENT_ID, USER_ID))
                .thenReturn(CommentDeleteVO.builder()
                        .deletedApprovedCount(3L)
                        .build());

        mockMvc.perform(delete("/api/v1/comments/{commentId}", COMMENT_ID)
                        .header("Authorization", "Bearer " + accessToken(USER_ID, UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.deletedApprovedCount").value(3));

        verify(commentService).deleteOwnComment(COMMENT_ID, USER_ID);
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
