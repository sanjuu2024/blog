package com.ccsanjuu.blog.modules.auth.controller;

import com.ccsanjuu.blog.modules.auth.model.dto.LoginRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.vo.LoginVO;
import com.ccsanjuu.blog.modules.auth.service.AuthService;
import com.ccsanjuu.blog.modules.auth.service.RegistrationEmailVerificationService;
import com.ccsanjuu.blog.modules.auth.support.RefreshTokenCookieManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private AuthService authService;
    private RegistrationEmailVerificationService registrationEmailVerificationService;
    private RefreshTokenCookieManager refreshTokenCookieManager;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        registrationEmailVerificationService = mock(RegistrationEmailVerificationService.class);
        refreshTokenCookieManager = mock(RefreshTokenCookieManager.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(
                authService,
                registrationEmailVerificationService,
                refreshTokenCookieManager
        )).build();
    }

    @Test
    void emailVerificationEndpointShouldPassEmailAndClientIp() throws Exception {
        mockMvc.perform(post("/auth/email-verification-codes")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(registrationEmailVerificationService)
                .sendCode("alice@example.com", "192.0.2.10");
    }

    @Test
    void loginEndpointShouldPassClientIpToAuthService() throws Exception {
        when(authService.login(any(LoginRequestDTO.class), eq("192.0.2.10")))
                .thenReturn(LoginVO.builder()
                        .refreshToken("refresh-token")
                        .refreshTokenExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1))
                        .build());

        mockMvc.perform(post("/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account":"alice@example.com","password":"Passw0rd!"}
                                """))
                .andExpect(status().isOk());

        verify(authService).login(any(LoginRequestDTO.class), eq("192.0.2.10"));
        verify(refreshTokenCookieManager).addRefreshTokenCookie(
                any(),
                eq("refresh-token"),
                any(OffsetDateTime.class)
        );
    }
}
