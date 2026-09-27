package com.ccsanjuu.blog.modules.dashboard.controller;

import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.config.SecurityConfig;
import com.ccsanjuu.blog.config.WebMvcConfig;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardSummaryVO;
import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardTrendsVO;
import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardVO;
import com.ccsanjuu.blog.modules.dashboard.service.DashboardService;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import com.ccsanjuu.blog.common.util.JwtUtil;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        WebMvcConfig.class,
        DashboardControllerTest.SecurityTestConfig.class
})
class DashboardControllerTest {

    private static final Long ADMIN_ID = 10001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey signingKey;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private TokenVersionService tokenVersionService;

    @BeforeEach
    void setUpTokenVersion() {
        when(tokenVersionService.getCurrentVersion(anyLong())).thenReturn(0L);
        when(dashboardService.getDashboard()).thenReturn(DashboardVO.builder()
                .summary(DashboardSummaryVO.builder().userCount(10L).build())
                .trends(DashboardTrendsVO.builder().build())
                .topByViews(java.util.List.of())
                .topByLikes(java.util.List.of())
                .topByComments(java.util.List.of())
                .build());
    }

    @Test
    void dashboardShouldRequireAdministrator() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + accessToken(UserRole.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorShouldReadDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + accessToken(UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.userCount").value(10));
    }

    private String accessToken(UserRole role) {
        return JwtUtil.generateAccessToken(
                signingKey,
                "sanjuu-blog",
                Duration.ofMinutes(15),
                ADMIN_ID,
                "admin",
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
