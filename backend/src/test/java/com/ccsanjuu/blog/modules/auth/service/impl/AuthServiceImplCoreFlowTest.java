package com.ccsanjuu.blog.modules.auth.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.common.util.TokenHashUtil;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.auth.model.dto.LoginRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.RefreshTokenRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.RegisterRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.entity.AuthSession;
import com.ccsanjuu.blog.modules.auth.model.enums.AuthSessionStatus;
import com.ccsanjuu.blog.modules.auth.model.enums.AuthSessionTokenType;
import com.ccsanjuu.blog.modules.auth.model.vo.LoginVO;
import com.ccsanjuu.blog.modules.auth.model.vo.RefreshTokenVO;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import com.ccsanjuu.blog.modules.privacy.service.PrivacyPolicyService;
import com.ccsanjuu.blog.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AuthServiceImplCoreFlowTest {

    private static final Long USER_ID = 10001L;
    private static final String PASSWORD = "Password_123";
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
        lenient().when(privacyPolicyService.compareTo(any())).thenReturn(true);
    }

    @Test
    void registerShouldCreateActiveUserWithEncodedPassword() {
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");

        authService.register(RegisterRequestDTO.builder()
                .username("Sanjuu")
                .email("sanjuu@example.com")
                .password(PASSWORD)
                .privacyPolicyVersion("sha256:" + "a".repeat(64))
                .build());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User user = userCaptor.getValue();
        assertEquals("Sanjuu", user.getUsername());
        assertEquals("Sanjuu", user.getNickname());
        assertEquals("sanjuu@example.com", user.getEmail());
        assertEquals("encoded-password", user.getPasswordHash());
        assertEquals(UserRole.USER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(0L, user.getTokenVersion());
    }

    @Test
    void registerShouldRejectDuplicateUsername() {
        when(userMapper.selectOne(any())).thenReturn(activeUser());

        BizException exception = assertThrows(BizException.class, () -> authService.register(
                RegisterRequestDTO.builder()
                        .username("sanjuu")
                        .email("another@example.com")
                        .password(PASSWORD)
                        .privacyPolicyVersion("sha256:" + "a".repeat(64))
                        .build()
        ));

        assertEquals(ResultCode.USERNAME_EXISTS, exception.getResultCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void registerShouldRejectWhenPrivacyPolicyVersionIsStale() {
        when(privacyPolicyService.compareTo(any())).thenReturn(false);

        BizException exception = assertThrows(BizException.class, () -> authService.register(
                RegisterRequestDTO.builder()
                        .username("sanjuu")
                        .email("sanjuu@example.com")
                        .password(PASSWORD)
                        .privacyPolicyVersion("sha256:" + "b".repeat(64))
                        .build()
        ));

        assertEquals(ResultCode.PRIVACY_POLICY_VERSION_MISMATCH, exception.getResultCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void loginShouldIssueTokensPersistSessionAndUpdateLoginTime() {
        User user = activeUser();
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.selectByIdForUpdate(USER_ID)).thenReturn(user);
        when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);

        LoginVO result = authService.login(LoginRequestDTO.builder()
                .account("SANJUU")
                .password(PASSWORD)
                .build());

        assertNotNull(result.getAccessToken());
        assertNotNull(result.getRefreshToken());
        assertEquals(USER_ID, result.getUser().getId());
        assertEquals(UserRole.USER, result.getUser().getRole());
        assertEquals(3L, JwtUtil.getTokenVersion(JwtUtil.parseClaims(result.getAccessToken(), SIGNING_KEY)));
        verify(userMapper).selectByIdForUpdate(USER_ID);
        verify(authMapper).insert(any(AuthSession.class));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(USER_ID, userCaptor.getValue().getId());
        assertNotNull(userCaptor.getValue().getLastLoginAt());
    }

    @Test
    void loginShouldRejectDisabledUserWithoutCreatingSession(CapturedOutput output) {
        String loginEmail = "sanjuu@example.com";
        User user = activeUser();
        user.setStatus(UserStatus.DISABLED);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.selectByIdForUpdate(USER_ID)).thenReturn(user);
        when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);

        BizException exception = assertThrows(BizException.class, () -> authService.login(
                LoginRequestDTO.builder().account(loginEmail).password(PASSWORD).build()
        ));

        assertEquals(ResultCode.USER_DISABLED, exception.getResultCode());
        verify(authMapper, never()).insert(any(AuthSession.class));
        assertTrue(output.getOut().contains("reason=USER_DISABLED"));
        assertTrue(output.getOut().contains("userId=" + USER_ID));
        assertTrue(output.getOut().contains("username=Sanjuu"));
        assertFalse(output.getOut().contains(loginEmail));
    }

    @Test
    void loginShouldValidatePasswordUsingLockedUserState(CapturedOutput output) {
        String loginEmail = "sanjuu@example.com";
        User queriedUser = activeUser();
        User lockedUser = activeUser();
        lockedUser.setPasswordHash("changed-password-hash");
        when(userMapper.selectOne(any())).thenReturn(queriedUser);
        when(userMapper.selectByIdForUpdate(USER_ID)).thenReturn(lockedUser);
        when(passwordEncoder.matches(PASSWORD, lockedUser.getPasswordHash())).thenReturn(false);

        BizException exception = assertThrows(BizException.class, () -> authService.login(
                LoginRequestDTO.builder().account(loginEmail).password(PASSWORD).build()
        ));

        assertEquals(ResultCode.PASSWORD_ERROR, exception.getResultCode());
        verify(userMapper).selectByIdForUpdate(USER_ID);
        verify(authMapper, never()).insert(any(AuthSession.class));
        assertTrue(output.getOut().contains("reason=PASSWORD_ERROR"));
        assertTrue(output.getOut().contains("userId=" + USER_ID));
        assertTrue(output.getOut().contains("username=Sanjuu"));
        assertFalse(output.getOut().contains(loginEmail));
    }

    @Test
    void loginShouldMaskUnmatchedAccountInput(CapturedOutput output) {
        String unknownEmail = "unknown@example.com";

        BizException exception = assertThrows(BizException.class, () -> authService.login(
                LoginRequestDTO.builder().account(unknownEmail).password(PASSWORD).build()
        ));

        assertEquals(ResultCode.USER_NOT_FOUND, exception.getResultCode());
        assertTrue(output.getOut().contains("reason=USER_NOT_FOUND"));
        assertTrue(output.getOut().contains("account=u***n@example.com"));
        assertFalse(output.getOut().contains(unknownEmail));
    }

    @Test
    void refreshShouldRevokeOldSessionAndPersistRotatedToken(CapturedOutput output) {
        String oldJti = "old-refresh-token-jti";
        String oldRefreshToken = JwtUtil.generateRefreshToken(
                SIGNING_KEY,
                "sanjuu-blog",
                Duration.ofDays(7),
                USER_ID,
                oldJti
        );
        AuthSession oldSession = AuthSession.builder()
                .userId(USER_ID)
                .tokenJti(oldJti)
                .tokenHash(TokenHashUtil.sha256(oldRefreshToken))
                .tokenType(AuthSessionTokenType.REFRESH)
                .status(AuthSessionStatus.ACTIVE)
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(7))
                .build();
        when(authMapper.selectOne(any())).thenReturn(oldSession);
        when(userMapper.selectByIdForUpdate(USER_ID)).thenReturn(activeUser());

        RefreshTokenVO result = authService.refresh(
                RefreshTokenRequestDTO.builder().refreshToken(oldRefreshToken).build()
        );

        assertNotNull(result.getAccessToken());
        assertNotNull(result.getRefreshToken());
        assertNotEquals(oldRefreshToken, result.getRefreshToken());
        verify(authMapper, org.mockito.Mockito.times(2)).selectOne(any());
        verify(authMapper).revokeRefreshToken(oldJti, AuthSessionStatus.REVOKED.getValue());
        verify(authMapper).insert(any(AuthSession.class));
        assertTrue(output.getOut().contains("security_event=TOKEN_REFRESH_SUCCESS"));
        assertTrue(output.getOut().contains("description=\"登录态刷新成功\""));
        assertFalse(output.getOut().contains(oldRefreshToken));
    }

    @Test
    void refreshShouldRejectDisabledUserWithoutRotatingSession(CapturedOutput output) {
        String refreshToken = JwtUtil.generateRefreshToken(
                SIGNING_KEY,
                "sanjuu-blog",
                Duration.ofDays(7),
                USER_ID,
                "disabled-user-session"
        );
        when(authMapper.selectOne(any())).thenReturn(AuthSession.builder()
                .userId(USER_ID)
                .tokenJti("disabled-user-session")
                .tokenHash(TokenHashUtil.sha256(refreshToken))
                .tokenType(AuthSessionTokenType.REFRESH)
                .status(AuthSessionStatus.ACTIVE)
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(7))
                .build());
        User disabledUser = activeUser();
        disabledUser.setStatus(UserStatus.DISABLED);
        when(userMapper.selectByIdForUpdate(USER_ID)).thenReturn(disabledUser);

        BizException exception = assertThrows(BizException.class, () -> authService.refresh(
                RefreshTokenRequestDTO.builder().refreshToken(refreshToken).build()
        ));

        assertEquals(ResultCode.USER_DISABLED, exception.getResultCode());
        verify(authMapper, never()).revokeRefreshToken(any(), any());
        verify(authMapper, never()).insert(any(AuthSession.class));
        assertTrue(output.getOut().contains("security_event=TOKEN_REFRESH_FAILED"));
        assertTrue(output.getOut().contains("reason=USER_DISABLED"));
        assertFalse(output.getOut().contains(refreshToken));
    }

    @Test
    void refreshShouldRejectTokenRevokedWhileWaitingForUserLock(CapturedOutput output) {
        String refreshToken = JwtUtil.generateRefreshToken(
                SIGNING_KEY,
                "sanjuu-blog",
                Duration.ofDays(7),
                USER_ID,
                "concurrent-refresh-session"
        );
        AuthSession activeSession = AuthSession.builder()
                .userId(USER_ID)
                .tokenJti("concurrent-refresh-session")
                .tokenHash(TokenHashUtil.sha256(refreshToken))
                .tokenType(AuthSessionTokenType.REFRESH)
                .status(AuthSessionStatus.ACTIVE)
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(7))
                .build();
        when(authMapper.selectOne(any()))
                .thenReturn(activeSession)
                .thenReturn((AuthSession) null);
        when(userMapper.selectByIdForUpdate(USER_ID)).thenReturn(activeUser());

        BizException exception = assertThrows(BizException.class, () -> authService.refresh(
                RefreshTokenRequestDTO.builder().refreshToken(refreshToken).build()
        ));

        assertEquals(ResultCode.REFRESH_TOKEN_INVALID_OR_EXPIRED, exception.getResultCode());
        verify(authMapper, never()).revokeRefreshToken(any(), any());
        verify(authMapper, never()).insert(any(AuthSession.class));
        assertTrue(output.getOut().contains("reason=REVOKED_WHILE_WAITING"));
        assertFalse(output.getOut().contains(refreshToken));
    }

    @Test
    void refreshShouldLogInvalidTokenWithoutExposingIt(CapturedOutput output) {
        String invalidRefreshToken = "invalid-refresh-token";

        BizException exception = assertThrows(BizException.class, () -> authService.refresh(
                RefreshTokenRequestDTO.builder().refreshToken(invalidRefreshToken).build()
        ));

        assertEquals(ResultCode.REFRESH_TOKEN_INVALID_OR_EXPIRED, exception.getResultCode());
        assertTrue(output.getOut().contains("security_event=TOKEN_REFRESH_FAILED"));
        assertTrue(output.getOut().contains("reason=INVALID_OR_EXPIRED"));
        assertFalse(output.getOut().contains(invalidRefreshToken));
    }

    private User activeUser() {
        return User.builder()
                .id(USER_ID)
                .username("Sanjuu")
                .nickname("Sanjuu")
                .email("sanjuu@example.com")
                .passwordHash("encoded-password")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .tokenVersion(3L)
                .build();
    }
}
