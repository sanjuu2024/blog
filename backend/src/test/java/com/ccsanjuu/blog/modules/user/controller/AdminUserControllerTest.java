package com.ccsanjuu.blog.modules.user.controller;

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
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.dto.UserManagementPageQueryDTO;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import com.ccsanjuu.blog.modules.user.model.vo.AdminUserItemVO;
import com.ccsanjuu.blog.modules.user.model.vo.UpdatedUserRoleVO;
import com.ccsanjuu.blog.modules.user.model.vo.UpdatedUserStatusVO;
import com.ccsanjuu.blog.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        WebMvcConfig.class,
        AdminUserControllerTest.SecurityTestConfig.class
})
class AdminUserControllerTest {

    private static final Long ADMIN_ID = 10001L;
    private static final Long USER_ID = 10002L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey signingKey;

    @MockitoBean
    private UserService userService;

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
    void userListShouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(101001));
    }

    @Test
    void userListShouldRejectNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + accessToken(USER_ID, UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(101003));
    }

    @Test
    void userListShouldBindFiltersForAdmin() throws Exception {
        when(userService.userPageQuery(any(UserManagementPageQueryDTO.class)))
                .thenReturn(PageResult.of(1L, 2L, 10L, List.of(AdminUserItemVO.builder()
                        .id(USER_ID)
                        .username("reader")
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build())));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + accessToken(ADMIN_ID, UserRole.ADMIN))
                        .queryParam("pageNum", "2")
                        .queryParam("pageSize", "10")
                        .queryParam("username", "reader")
                        .queryParam("email", "example.com")
                        .queryParam("role", "USER")
                        .queryParam("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(USER_ID));

        verify(userService).userPageQuery(argThat(query ->
                Integer.valueOf(2).equals(query.getPageNum())
                        && Integer.valueOf(10).equals(query.getPageSize())
                        && "reader".equals(query.getUsername())
                        && "example.com".equals(query.getEmail())
                        && query.getRole() == UserRole.USER
                        && query.getStatus() == UserStatus.ACTIVE
        ));
    }

    @Test
    void changeStatusShouldPassAdminAndTargetUserIds() throws Exception {
        when(userService.changeUserStatus(eq(ADMIN_ID), eq(USER_ID), any()))
                .thenReturn(UpdatedUserStatusVO.builder()
                        .id(USER_ID)
                        .status(UserStatus.DISABLED)
                        .build());

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", USER_ID)
                        .header("Authorization", "Bearer " + accessToken(ADMIN_ID, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(USER_ID))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        verify(userService).changeUserStatus(eq(ADMIN_ID), eq(USER_ID), any());
    }

    @Test
    void changeRoleShouldPassAdminAndTargetUserIds() throws Exception {
        when(userService.changeUserRole(eq(ADMIN_ID), eq(USER_ID), any()))
                .thenReturn(UpdatedUserRoleVO.builder()
                        .id(USER_ID)
                        .role(UserRole.ADMIN)
                        .build());

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/role", USER_ID)
                        .header("Authorization", "Bearer " + accessToken(ADMIN_ID, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(USER_ID))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        verify(userService).changeUserRole(eq(ADMIN_ID), eq(USER_ID), any());
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
