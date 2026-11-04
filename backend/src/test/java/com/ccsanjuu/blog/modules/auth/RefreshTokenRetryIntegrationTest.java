package com.ccsanjuu.blog.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.common.util.TokenHashUtil;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.auth.model.dto.LogoutRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.RefreshTokenRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.entity.AuthSession;
import com.ccsanjuu.blog.modules.auth.model.enums.AuthSessionStatus;
import com.ccsanjuu.blog.modules.auth.model.enums.AuthSessionTokenType;
import com.ccsanjuu.blog.modules.auth.model.vo.RefreshTokenVO;
import com.ccsanjuu.blog.modules.auth.service.AuthService;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshTokenRetryIntegrationTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private AuthMapper authMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TokenVersionService tokenVersionService;
    @Autowired
    private SecretKey jwtSigningKey;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private String previousToken;
    private final Set<String> retryKeys = new HashSet<>();

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        user = User.builder().username("rt_" + suffix).nickname("轮转测试")
                .email("rt_" + suffix + "@example.test").passwordHash("unused-test-password")
                .role(UserRole.USER).status(UserStatus.ACTIVE).tokenVersion(0L).build();
        userMapper.insert(user);
        String jti = UUID.randomUUID().toString();
        previousToken = JwtUtil.generateRefreshToken(jwtSigningKey, "sanjuu-blog", Duration.ofDays(7), user.getId(), jti);
        authMapper.insert(AuthSession.builder().userId(user.getId()).tokenJti(jti)
                .tokenHash(TokenHashUtil.sha256(previousToken)).tokenType(AuthSessionTokenType.REFRESH)
                .status(AuthSessionStatus.ACTIVE).expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(7)).build());
        retryKeys.add(retryKey(previousToken));
    }

    @AfterEach
    void cleanUp() {
        redis.delete(retryKeys);
        if (user != null && user.getId() != null) {
            redis.delete("blog:auth:token-version:user:" + user.getId());
            authMapper.delete(new LambdaQueryWrapper<AuthSession>().eq(AuthSession::getUserId, user.getId()));
            jdbc.update("DELETE FROM blog_security_event WHERE user_id = ?", user.getId());
            userMapper.deleteById(user.getId());
        }
    }

    @Test
    void lostResponseRetryShouldReturnSameTokensAndCookieWithoutMoreSessions() throws Exception {
        var firstResponse = mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", previousToken)))
                .andExpect(status().isOk()).andReturn().getResponse();
        var first = objectMapper.readTree(firstResponse.getContentAsString()).get("data");
        assertFalse(first.has("refreshToken"));
        Cookie firstCookie = firstResponse.getCookie("refresh_token");
        assertNotNull(firstCookie);
        long ttl = redis.getExpire(retryKey(previousToken), TimeUnit.MILLISECONDS);

        var repeatedResponse = mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", previousToken)))
                .andExpect(status().isOk())
                .andExpect(cookie().value("refresh_token", firstCookie.getValue()))
                .andReturn().getResponse();

        assertEquals(first, objectMapper.readTree(repeatedResponse.getContentAsString()).get("data"));
        assertTrue(ttl > 0 && ttl <= 5000);
        assertTrue(redis.getExpire(retryKey(previousToken), TimeUnit.MILLISECONDS) <= ttl);
        assertEquals(2, sessionCount());
    }

    @Test
    void concurrentRequestsWithSamePreviousTokenShouldOnlyRotateOnce() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return refresh(previousToken);
            });
            var second = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return refresh(previousToken);
            });
            start.countDown();

            assertEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }
        assertEquals(2, sessionCount());
    }

    @Test
    void logoutUsingPreviousCookieShouldInvalidateBothTokens() {
        var tokens = refresh(previousToken);

        authService.logout(LogoutRequestDTO.builder().refreshToken(previousToken).build());

        assertInvalid(previousToken);
        assertInvalid(tokens.getRefreshToken());
    }

    @Test
    void logoutUsingCurrentCookieShouldPreventRetryFromRestoringSession() {
        var tokens = refresh(previousToken);

        authService.logout(LogoutRequestDTO.builder().refreshToken(tokens.getRefreshToken()).build());

        assertInvalid(previousToken);
        assertInvalid(tokens.getRefreshToken());
    }

    @Test
    void securityVersionChangeShouldInvalidateCachedResult() {
        var tokens = refresh(previousToken);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            tokenVersionService.incrementVersion(user.getId());
            authService.revokeUserRefreshTokens(user.getId());
        });

        assertInvalid(previousToken);
        assertInvalid(tokens.getRefreshToken());
    }

    @Test
    void rollbackShouldNotMakeCachedUncommittedSuccessorUsable() {
        AtomicReference<RefreshTokenVO> rolledBack = new AtomicReference<>();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            rolledBack.set(refresh(previousToken));
            status.setRollbackOnly();
        });

        var committed = refresh(previousToken);

        assertNotNull(rolledBack.get());
        assertNotEquals(rolledBack.get().getRefreshToken(), committed.getRefreshToken());
        assertInvalid(rolledBack.get().getRefreshToken());
        assertEquals(committed, refresh(previousToken));
        assertEquals(2, sessionCount());
    }

    @Test
    void expiredRetryWindowShouldRejectEvenIfRedisEntryRemains() {
        refresh(previousToken);
        jdbc.update("UPDATE blog_auth_session SET revoked_at = ? WHERE token_jti = ?",
                OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(6), JwtUtil.getTokenId(previousToken, jwtSigningKey));

        assertInvalid(previousToken);
    }

    @Test
    void furtherRotationShouldNotAllowReturningAnAlreadyRevokedSuccessor() {
        var first = refresh(previousToken);
        retryKeys.add(retryKey(first.getRefreshToken()));
        var second = refresh(first.getRefreshToken());

        assertInvalid(previousToken);
        assertEquals(second, refresh(first.getRefreshToken()));
        assertEquals(3, sessionCount());
    }

    @Test
    void missingCacheShouldRejectPreviousTokenButKeepCurrentSessionUsable() {
        var first = refresh(previousToken);
        redis.delete(retryKey(previousToken));

        assertInvalid(previousToken);
        retryKeys.add(retryKey(first.getRefreshToken()));
        assertNotNull(refresh(first.getRefreshToken()));
    }

    private RefreshTokenVO refresh(String token) {
        return authService.refresh(RefreshTokenRequestDTO.builder().refreshToken(token).build());
    }

    private void assertInvalid(String token) {
        BizException exception = assertThrows(BizException.class, () -> refresh(token));
        assertEquals(ResultCode.REFRESH_TOKEN_INVALID_OR_EXPIRED, exception.getResultCode());
    }

    private int sessionCount() {
        return Math.toIntExact(authMapper.selectCount(new LambdaQueryWrapper<AuthSession>()
                .eq(AuthSession::getUserId, user.getId())));
    }

    private String retryKey(String token) {
        return "blog:auth:refresh-retry:" + TokenHashUtil.sha256(token);
    }
}
