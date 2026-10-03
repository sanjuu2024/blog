package com.ccsanjuu.blog.modules.auth.controller;

import com.ccsanjuu.blog.modules.auth.service.AuthService;
import com.ccsanjuu.blog.modules.auth.service.RegistrationEmailVerificationService;
import com.ccsanjuu.blog.modules.auth.support.RefreshTokenCookieManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private RegistrationEmailVerificationService registrationEmailVerificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthService authService = mock(AuthService.class);
        registrationEmailVerificationService = mock(RegistrationEmailVerificationService.class);
        RefreshTokenCookieManager refreshTokenCookieManager = mock(RefreshTokenCookieManager.class);
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
}
