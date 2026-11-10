package com.ccsanjuu.blog.modules.auth.service.impl;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.common.util.TokenHashUtil;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.auth.model.bo.RefreshTokenRetryBO;
import com.ccsanjuu.blog.modules.auth.model.dto.LogoutRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.RefreshTokenRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.entity.AuthSession;
import com.ccsanjuu.blog.modules.auth.model.enums.AuthSessionStatus;
import com.ccsanjuu.blog.modules.auth.model.enums.AuthSessionTokenType;
import com.ccsanjuu.blog.modules.auth.service.RegistrationEmailVerificationService;
import com.ccsanjuu.blog.modules.auth.support.LoginRateLimiter;
import com.ccsanjuu.blog.modules.auth.support.RefreshTokenRetryCache;
import com.ccsanjuu.blog.modules.privacy.service.PrivacyPolicyService;
import com.ccsanjuu.blog.modules.security.service.SecurityEventService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import com.ccsanjuu.blog.properties.JwtProperties;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class AuthServiceImplRefreshRetryTest {

    private static final Long USER_ID = 10001L;
    private static final SecretKey KEY = JwtUtil.createHmacShaKey("0123456789abcdef0123456789abcdef");

    @Mock
    private UserMapper userMapper;
    @Mock
    private AuthMapper authMapper;
    @Mock
    private RefreshTokenRetryCache retryCache;

    private AuthServiceImpl service;
    private String previousToken;
    private String successorToken;
    private AuthSession previousSession;
    private User user;

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(AuthSession.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            assistant.setCurrentNamespace(AuthSession.class.getName());
            TableInfoHelper.initTableInfo(assistant, AuthSession.class);
        }
    }

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userMapper, mock(PasswordEncoder.class),
                new JwtProperties("blog-test", "unused", Duration.ofMinutes(15), Duration.ofDays(7)),
                KEY, authMapper, mock(PrivacyPolicyService.class),
                mock(RegistrationEmailVerificationService.class), mock(SecurityEventService.class),
                mock(LoginRateLimiter.class), retryCache,
                new SimpleMeterRegistry());
        previousToken = JwtUtil.generateRefreshToken(KEY, "blog-test", Duration.ofDays(7), USER_ID, "previous");
        successorToken = JwtUtil.generateRefreshToken(KEY, "blog-test", Duration.ofDays(7), USER_ID, "successor");
        previousSession = session(previousToken, "previous", AuthSessionStatus.REVOKED);
        previousSession.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
        user = User.builder().id(USER_ID).username("reader").role(UserRole.USER)
                .status(UserStatus.ACTIVE).tokenVersion(2L).build();
    }

    @Test
    void retryShouldReturnIdenticalTokensWithoutRotatingOrExtendingWindow() {
        stubPrevious();
        when(authMapper.selectRefreshSession("successor", USER_ID))
                .thenReturn(session(successorToken, "successor", AuthSessionStatus.ACTIVE));
        RefreshTokenRetryBO cached = cachedResult(2L);
        when(retryCache.find(previousToken)).thenReturn(cached);

        var first = service.refresh(request());
        var second = service.refresh(request());

        assertEquals(cached.accessToken(), first.getAccessToken());
        assertEquals(successorToken, first.getRefreshToken());
        assertEquals(first, second);
        verifyNoRotation();
        verify(retryCache, never()).store(any(), any(Long.class), any(), any());
    }

    @Test
    void ordinaryRevocationWithoutRotationResultShouldBeRejected() {
        stubPrevious();

        assertInvalidRefresh();

        verifyNoRotation();
    }

    @Test
    void retryShouldRejectAfterFixedWindowEvenWhenCacheStillExists() {
        previousSession.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(6));
        when(authMapper.selectRefreshSession("previous", USER_ID)).thenReturn(previousSession);

        assertInvalidRefresh();

        verify(retryCache, never()).find(any());
        verifyNoRotation();
    }

    @Test
    void retryShouldRejectChangedTokenVersion() {
        stubPrevious();
        when(retryCache.find(previousToken)).thenReturn(cachedResult(1L));

        assertInvalidRefresh();

        verify(authMapper, never()).selectRefreshSession("successor", USER_ID);
        verifyNoRotation();
    }

    @Test
    void retryShouldRejectSuccessorRevokedByLogoutOrFurtherRotation() {
        stubPrevious();
        when(retryCache.find(previousToken)).thenReturn(cachedResult(2L));
        when(authMapper.selectRefreshSession("successor", USER_ID))
                .thenReturn(session(successorToken, "successor", AuthSessionStatus.REVOKED));

        assertInvalidRefresh();

        verifyNoRotation();
    }

    @Test
    void retryShouldRejectMissingSuccessorAfterTransactionRollback() {
        stubPrevious();
        when(retryCache.find(previousToken)).thenReturn(cachedResult(2L));

        assertInvalidRefresh();

        verifyNoRotation();
    }

    @Test
    void retryShouldRejectDisabledUserBeforeReadingCachedTokens() {
        stubPrevious();
        user.setStatus(UserStatus.DISABLED);

        BizException exception = assertThrows(BizException.class, () -> service.refresh(request()));

        assertEquals(ResultCode.USER_DISABLED, exception.getResultCode());
        verify(retryCache, never()).find(any());
        verifyNoRotation();
    }

    @Test
    void retryShouldRejectDeletedUserBeforeReadingCachedTokens() {
        stubPrevious();
        user.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));

        BizException exception = assertThrows(BizException.class, () -> service.refresh(request()));

        assertEquals(ResultCode.USER_DELETED, exception.getResultCode());
        verify(retryCache, never()).find(any());
        verifyNoRotation();
    }

    @Test
    void retryShouldCheckOldTokenHashBeforeConsultingCache() {
        previousSession.setTokenHash("wrong-hash");
        when(authMapper.selectRefreshSession("previous", USER_ID)).thenReturn(previousSession);

        assertInvalidRefresh();

        verify(retryCache, never()).find(any());
        verifyNoRotation();
    }

    @Test
    void logoutWithPreviousTokenShouldRevokeItsActiveSuccessor() {
        stubPrevious();
        when(retryCache.find(previousToken)).thenReturn(cachedResult(2L));
        when(authMapper.selectRefreshSession("successor", USER_ID))
                .thenReturn(session(successorToken, "successor", AuthSessionStatus.ACTIVE));

        service.logout(LogoutRequestDTO.builder().refreshToken(previousToken).build());

        ArgumentCaptor<LambdaUpdateWrapper<AuthSession>> updates = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(authMapper, times(2)).update(any(AuthSession.class), updates.capture());
        assertTrue(updates.getAllValues().stream().anyMatch(update -> {
            update.getSqlSegment();
            return update.getParamNameValuePairs().containsValue("successor");
        }));
        verify(authMapper, never()).insert(any(AuthSession.class));
    }

    private AuthSession session(String token, String jti, AuthSessionStatus status) {
        return AuthSession.builder().userId(USER_ID).tokenJti(jti).tokenType(AuthSessionTokenType.REFRESH)
                .tokenHash(TokenHashUtil.sha256(token)).status(status)
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(7)).build();
    }

    private RefreshTokenRetryBO cachedResult(long version) {
        return new RefreshTokenRetryBO(version, "same-access-token",
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15), successorToken,
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(7), Instant.now().plusSeconds(5),
                TokenHashUtil.sha256(previousToken));
    }

    private RefreshTokenRequestDTO request() {
        return RefreshTokenRequestDTO.builder().refreshToken(previousToken).build();
    }

    private void stubPrevious() {
        when(authMapper.selectRefreshSession("previous", USER_ID)).thenReturn(previousSession);
        when(userMapper.selectByIdForUpdate(USER_ID)).thenReturn(user);
    }

    private void assertInvalidRefresh() {
        BizException exception = assertThrows(BizException.class, () -> service.refresh(request()));
        assertEquals(ResultCode.REFRESH_TOKEN_INVALID_OR_EXPIRED, exception.getResultCode());
    }

    private void verifyNoRotation() {
        verify(authMapper, never()).insert(any(AuthSession.class));
        verify(authMapper, never()).update(any(AuthSession.class), any());
    }
}
