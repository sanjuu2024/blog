package com.ccsanjuu.blog.modules.message.controller;

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
import com.ccsanjuu.blog.modules.audit.mapper.AdminAuditLogMapper;
import com.ccsanjuu.blog.modules.message.model.dto.MessagePageQueryDTO;
import com.ccsanjuu.blog.modules.message.model.enums.MessageStatus;
import com.ccsanjuu.blog.modules.message.model.enums.MessageType;
import com.ccsanjuu.blog.modules.message.model.vo.AdminMessageItemVO;
import com.ccsanjuu.blog.modules.message.service.MessageService;
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
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminMessageController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        WebMvcConfig.class,
        AdminMessageControllerTest.SecurityTestConfig.class
})
class AdminMessageControllerTest {

    private static final Long ADMIN_ID = 10001L;
    private static final Long USER_ID = 10002L;
    private static final Long MESSAGE_ID = 90001L;
    private static final OffsetDateTime CREATED_AT_FROM = OffsetDateTime.parse("2026-08-01T00:00:00+08:00");

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
    private CommentMapper commentMapper;

    @MockitoBean
    private MessageMapper messageMapper;

    @MockitoBean
    private AdminAuditLogMapper adminAuditLogMapper;

    @MockitoBean
    private TagMapper tagMapper;

    @MockitoBean
    private UserMapper userMapper;

    @BeforeEach
    void setUpTokenVersion() {
        when(tokenVersionService.getCurrentVersion(anyLong())).thenReturn(0L);
    }

    @Test
    void adminMessageListShouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/messages"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(101001));
    }

    @Test
    void adminMessageListShouldBindFiltersForAdmin() throws Exception {
        when(messageService.getAdminMessageList(any(MessagePageQueryDTO.class)))
                .thenReturn(PageResult.of(1L, 1L, 10L, List.of(AdminMessageItemVO.builder()
                        .id(MESSAGE_ID)
                        .status(MessageStatus.PENDING)
                        .type(MessageType.TOP_LEVEL)
                        .build())));

        mockMvc.perform(get("/api/v1/admin/messages")
                        .header("Authorization", "Bearer " + accessToken())
                        .queryParam("messageId", MESSAGE_ID.toString())
                        .queryParam("userId", USER_ID.toString())
                        .queryParam("status", "PENDING")
                        .queryParam("type", "TOP_LEVEL")
                        .queryParam("createdAtFrom", CREATED_AT_FROM.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(MESSAGE_ID));

        ArgumentCaptor<MessagePageQueryDTO> captor = ArgumentCaptor.forClass(MessagePageQueryDTO.class);
        verify(messageService).getAdminMessageList(captor.capture());
        assertEquals(MESSAGE_ID, captor.getValue().getMessageId());
        assertEquals(USER_ID, captor.getValue().getUserId());
        assertEquals(MessageStatus.PENDING, captor.getValue().getStatus());
        assertEquals(MessageType.TOP_LEVEL, captor.getValue().getType());
        assertEquals(CREATED_AT_FROM, captor.getValue().getCreatedAtFrom());
    }

    @Test
    void batchApprovalShouldPassAdminAndMessageIds() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/messages/batch-approval")
                        .header("Authorization", "Bearer " + accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageIds\":[90001,90002]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(messageService).approveMessages(anyLong(), any());
    }

    @Test
    void batchApprovalShouldRejectEmptyMessageIds() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/messages/batch-approval")
                        .header("Authorization", "Bearer " + accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001));
    }

    private String accessToken() {
        return JwtUtil.generateAccessToken(
                signingKey,
                "sanjuu-blog",
                Duration.ofMinutes(15),
                ADMIN_ID,
                "admin",
                UserRole.ADMIN.getValue(),
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
