package com.ccsanjuu.blog.config;

import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.modules.auth.controller.AuthController;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.auth.support.RefreshTokenCookieManager;
import com.ccsanjuu.blog.modules.auth.service.AuthService;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@WebMvcTest(controllers = {
        AuthController.class
})
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        ApiErrorHandlingTest.ValidationTestController.class
})
class ApiErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenCookieManager refreshTokenCookieManager;

    @MockitoBean
    private AuthMapper authMapper;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private CategoryMapper categoryMapper;

    @MockitoBean
    private SecretKey jwtSigningKey;

    @Test
    void shouldReturnParamInvalidWhenRequestBodyValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "email": "not-an-email",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001))
                .andExpect(jsonPath("$.message").value("请求参数不合法"))
                .andExpect(jsonPath("$.data[0].field").exists());
    }

    @Test
    void shouldReturnParamInvalidWhenRequestBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001))
                .andExpect(jsonPath("$.message").value("请求参数不合法"))
                .andExpect(jsonPath("$.data[0].field").value("requestBody"));
    }

    @Test
    void shouldReturnParamInvalidWhenRequestParamTypeMismatch() throws Exception {
        mockMvc.perform(get("/api/v1/validation/number")
                        .param("pageNum", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001))
                .andExpect(jsonPath("$.message").value("请求参数不合法"))
                .andExpect(jsonPath("$.data[0].field").value("pageNum"));
    }

    @Test
    void shouldReturnUsernameExistsWhenUsernameUniqueIndexConflicts() throws Exception {
        mockMvc.perform(post("/api/v1/validation/duplicate-username"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(102002))
                .andExpect(jsonPath("$.message").value("用户名已存在"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void shouldReturnEmailExistsWhenEmailUniqueIndexConflicts() throws Exception {
        mockMvc.perform(post("/api/v1/validation/duplicate-email"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(102004))
                .andExpect(jsonPath("$.message").value("邮箱已存在"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void shouldReturnUnauthorizedJsonWhenAnonymousAccessesProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(101001))
                .andExpect(jsonPath("$.message").value("未登录或 Access Token 无效"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void shouldLogoutSuccessfullyWhenRefreshTokenCookieIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("成功"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        verify(authService).logout(argThat(logoutRequestDTO ->
                logoutRequestDTO.getRefreshToken() == null));
        verify(refreshTokenCookieManager).clearRefreshTokenCookie(any());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void shouldReturnForbiddenJsonWhenUserAccessesAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(101003))
                .andExpect(jsonPath("$.message").value("无权限访问"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @RestController
    @RequestMapping("/api/v1")
    @Validated
    public static class ValidationTestController {

        @GetMapping("/validation/number")
        public String validateNumber(@RequestParam @NotNull Integer pageNum) {
            return String.valueOf(pageNum);
        }

        @PostMapping("/validation/duplicate-username")
        public void duplicateUsername() {
            throw new DuplicateKeyException(
                    "duplicate key value violates unique constraint \"uq_blog_user_username_lower\"");
        }

        @PostMapping("/validation/duplicate-email")
        public void duplicateEmail() {
            throw new DuplicateKeyException(
                    "duplicate key value violates unique constraint \"uq_blog_user_email_lower\"");
        }
    }
}
