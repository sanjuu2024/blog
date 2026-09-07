package com.ccsanjuu.blog.modules.message.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
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
import com.ccsanjuu.blog.modules.message.model.vo.MessageMutationVO;
import com.ccsanjuu.blog.modules.message.model.vo.PublicMessageItemVO;
import com.ccsanjuu.blog.modules.message.service.MessageService;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MessageController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        WebMvcConfig.class,
        MessageControllerTest.SecurityTestConfig.class
})
class MessageControllerTest {

    private static final Long USER_ID = 10002L;
    private static final Long MESSAGE_ID = 90001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey signingKey;

    @MockitoBean
    private MessageService messageService;

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
    private MessageMapper messageMapper;

    @MockitoBean
    private CommentMapper commentMapper;

    @MockitoBean
    private TagMapper tagMapper;

    @MockitoBean
    private UserMapper userMapper;

    @BeforeEach
    void setUpTokenVersion() {
        when(tokenVersionService.getCurrentVersion(anyLong())).thenReturn(0L);
    }

    @Test
    void guestPostShouldBePublicAndPassClientIpToService() throws Exception {
        when(messageService.createMessage(eq(null), any(String.class), any()))
                .thenReturn(MessageMutationVO.builder().id(MESSAGE_ID).build());

        mockMvc.perform(post("/api/v1/messages")
                        .contentType("application/json")
                        .content("{\"nickname\":\"访客\",\"content\":\"留言\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(MESSAGE_ID));

        verify(messageService).createMessage(eq(null), any(String.class), any());
    }

    @Test
    void guestGetShouldRemainPublic() throws Exception {
        when(messageService.getPublicMessageList(eq(null), any()))
                .thenReturn(PageResult.of(0L, 1L, 10L, List.of()));

        mockMvc.perform(get("/api/v1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isEmpty());

        verify(messageService).getPublicMessageList(eq(null), any());
    }

    @Test
    void authenticatedGetAndPostShouldPassCurrentUser() throws Exception {
        when(messageService.getPublicMessageList(eq(USER_ID), any()))
                .thenReturn(PageResult.of(1L, 1L, 10L, List.of(PublicMessageItemVO.builder()
                        .id(MESSAGE_ID)
                        .build())));
        when(messageService.createMessage(eq(USER_ID), any(String.class), any()))
                .thenReturn(MessageMutationVO.builder().id(MESSAGE_ID).build());
        String authorization = "Bearer " + accessToken(USER_ID, UserRole.USER);

        mockMvc.perform(get("/api/v1/messages").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(MESSAGE_ID));
        mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", authorization)
                        .contentType("application/json")
                        .content("{\"content\":\"登录用户留言\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(MESSAGE_ID));

        verify(messageService).getPublicMessageList(eq(USER_ID), any());
        verify(messageService).createMessage(eq(USER_ID), any(String.class), any());
    }

    @Test
    void invalidAccessTokenShouldNotFallBackToGuestOnGetOrPost() throws Exception {
        String authorization = "Bearer not-a-valid-jwt";

        mockMvc.perform(get("/api/v1/messages").header("Authorization", authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(101001));
        mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", authorization)
                        .contentType("application/json")
                        .content("{\"content\":\"登录用户留言\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(101001));

        verifyNoInteractions(messageService);
    }

    @Test
    void guestPostShouldRejectInvalidEmailFromService() throws Exception {
        when(messageService.createMessage(eq(null), any(String.class), any()))
                .thenThrow(new BizException(ResultCode.PARAM_INVALID));

        mockMvc.perform(post("/api/v1/messages")
                        .contentType("application/json")
                        .content("{\"nickname\":\"访客\",\"email\":\"not-an-email\",\"content\":\"留言\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001));
    }

    @Test
    void deleteShouldRequireLogin() throws Exception {
        mockMvc.perform(delete("/api/v1/messages/{messageId}", MESSAGE_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(101001));
    }

    @Test
    void authenticatedDeleteShouldPassCurrentUser() throws Exception {
        mockMvc.perform(delete("/api/v1/messages/{messageId}", MESSAGE_ID)
                        .header("Authorization", "Bearer " + accessToken(USER_ID, UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(messageService).deleteOwnMessage(MESSAGE_ID, USER_ID);
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
