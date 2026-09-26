package com.ccsanjuu.blog.modules.auth.service.impl;

import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.auth.model.dto.LogoutRequestDTO;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.privacy.service.PrivacyPolicyService;
import com.ccsanjuu.blog.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKey;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplLogoutTest {

    private static final SecretKey SIGNING_KEY =
            JwtUtil.createHmacShaKey("0123456789abcdef0123456789abcdef");

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private PrivacyPolicyService privacyPolicyService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(
                "sanjuu-blog",
                "unused-in-this-test",
                Duration.ofMinutes(15),
                Duration.ofDays(7)
        );
        authService = new AuthServiceImpl(
                userMapper,
                passwordEncoder,
                jwtProperties,
                SIGNING_KEY,
                authMapper,
                privacyPolicyService
        );
    }

    @Test
    void logoutShouldSucceedWhenRefreshTokenIsInvalid() {
        assertDoesNotThrow(() -> authService.logout(
                LogoutRequestDTO.builder()
                        .refreshToken("not-a-jwt")
                        .build()
        ));
        verifyNoInteractions(authMapper);
    }
}
