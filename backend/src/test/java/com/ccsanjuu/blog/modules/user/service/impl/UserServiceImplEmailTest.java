package com.ccsanjuu.blog.modules.user.service.impl;

import com.ccsanjuu.blog.modules.auth.model.enums.EmailVerificationPurpose;
import com.ccsanjuu.blog.modules.auth.service.AuthService;
import com.ccsanjuu.blog.modules.auth.service.RegistrationEmailVerificationService;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.file.service.ImageUploadService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.dto.ChangeEmailRequestDTO;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.vo.UpdatedUserProfileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplEmailTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuthService authService;
    @Mock
    private TokenVersionService tokenVersionService;
    @Mock
    private ImageUploadService imageUploadService;
    @Mock
    private RegistrationEmailVerificationService emailVerificationService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                passwordEncoder, userMapper, authService, tokenVersionService,
                imageUploadService, emailVerificationService,
                org.mockito.Mockito.mock(com.ccsanjuu.blog.modules.security.service.SecurityEventService.class));
    }

    @Test
    void sendEmailChangeCodeShouldUseEmailChangePurpose() {
        User user = User.builder().id(10001L).email("old@example.com").build();
        when(userMapper.selectById(10001L)).thenReturn(user);
        when(userMapper.selectCount(any())).thenReturn(0L);

        userService.sendEmailChangeCode(10001L, "new@example.com", "127.0.0.1");

        verify(emailVerificationService).sendCode(
                "new@example.com", "127.0.0.1", EmailVerificationPurpose.EMAIL_CHANGE);
    }

    @Test
    void changeEmailShouldVerifyCodeUpdateUserAndInvalidateSessions() {
        User user = User.builder()
                .id(10001L)
                .email("old@example.com")
                .passwordHash("password-hash")
                .nickname("Alice")
                .bio("bio")
                .build();
        when(userMapper.selectByIdForUpdate(10001L)).thenReturn(user);
        when(passwordEncoder.matches("OldPassword_123", "password-hash")).thenReturn(true);
        when(userMapper.selectCount(any())).thenReturn(0L);

        ChangeEmailRequestDTO request = new ChangeEmailRequestDTO(
                "OldPassword_123", "new@example.com", "123456");

        UpdatedUserProfileVO result = userService.changeEmail(10001L, request);

        verify(emailVerificationService).verifyCode(
                "new@example.com", "123456", EmailVerificationPurpose.EMAIL_CHANGE);
        verify(userMapper).updateById(any(User.class));
        verify(tokenVersionService).incrementVersion(10001L);
        verify(authService).revokeUserRefreshTokens(10001L);
        assertEquals("new@example.com", result.getEmail());
    }
}
