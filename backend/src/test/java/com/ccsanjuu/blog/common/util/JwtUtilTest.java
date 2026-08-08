package com.ccsanjuu.blog.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String RAW_SECRET = "0123456789abcdef0123456789abcdef";
    private static final SecretKey SIGNING_KEY = JwtUtil.createHmacShaKey(RAW_SECRET);
    private static final String ISSUER = "sanjuu-blog";

    @Test
    void shouldGenerateAndParseAccessToken() {
        String token = JwtUtil.generateAccessToken(
                SIGNING_KEY,
                ISSUER,
                Duration.ofMinutes(15),
                10001L,
                "sanjuu",
                "ADMIN",
                "ACTIVE"
        );

        assertNotNull(token);
        Claims claims = JwtUtil.parseClaims(token, SIGNING_KEY);
        assertEquals(10001L, JwtUtil.getUserId(token, SIGNING_KEY));
        assertEquals(10001L, JwtUtil.getUserId(claims));
        assertEquals("sanjuu", JwtUtil.getUsername(token, SIGNING_KEY));
        assertEquals("ADMIN", JwtUtil.getRole(token, SIGNING_KEY));
        assertEquals("ACTIVE", JwtUtil.getStatus(token, SIGNING_KEY));
        assertEquals(0L, JwtUtil.getTokenVersion(claims));
        assertTrue(JwtUtil.isAccessToken(token, SIGNING_KEY));
        assertTrue(JwtUtil.isTokenValid(token, SIGNING_KEY, JwtUtil.TOKEN_TYPE_ACCESS, 10001L));
    }

    @Test
    void shouldStoreTokenVersionInAccessToken() {
        String token = JwtUtil.generateAccessToken(
                SIGNING_KEY,
                ISSUER,
                Duration.ofMinutes(15),
                10001L,
                "sanjuu",
                "ADMIN",
                "ACTIVE",
                3L
        );

        Claims claims = JwtUtil.parseClaims(token, SIGNING_KEY);

        assertEquals(3L, JwtUtil.getTokenVersion(claims));
    }

    @Test
    void shouldGenerateAndParseRefreshToken() {
        String token = JwtUtil.generateRefreshToken(
                SIGNING_KEY,
                ISSUER,
                Duration.ofDays(7),
                10001L,
                "refresh-jti-001"
        );

        assertEquals(10001L, JwtUtil.getUserId(token, SIGNING_KEY));
        assertEquals("refresh-jti-001", JwtUtil.getTokenId(token, SIGNING_KEY));
        assertTrue(JwtUtil.isRefreshToken(token, SIGNING_KEY));
        assertTrue(JwtUtil.isTokenValid(token, SIGNING_KEY, JwtUtil.TOKEN_TYPE_REFRESH, 10001L));
    }

    @Test
    void shouldSupportBase64Secret() {
        // 生产配置里常见的是 Base64 形式的密钥，而不是直接写原始文本。
        String base64Secret = Base64.getEncoder()
                .encodeToString(RAW_SECRET.getBytes(StandardCharsets.UTF_8));
        SecretKey key = JwtUtil.createHmacShaKeyFromBase64(base64Secret);

        String token = JwtUtil.generateAccessToken(
                key,
                ISSUER,
                Duration.ofMinutes(5),
                20002L,
                "alice",
                "USER",
                "ACTIVE"
        );

        assertEquals(20002L, JwtUtil.getUserId(token, key));
        assertEquals("alice", JwtUtil.getUsername(token, key));
    }

    @Test
    void shouldTreatExpiredTokenAsInvalid() {
        // 使用负 ttl 可以直接构造一个已过期 token，避免测试里等待时间流逝。
        String token = JwtUtil.generateToken(
                SIGNING_KEY,
                ISSUER,
                Duration.ofSeconds(-1),
                "10001",
                "expired-jti-001",
                Map.of(
                        JwtUtil.CLAIM_USER_ID, 10001L,
                        JwtUtil.CLAIM_TOKEN_TYPE, JwtUtil.TOKEN_TYPE_ACCESS
                )
        );

        assertTrue(JwtUtil.isTokenExpired(token, SIGNING_KEY));
        assertFalse(JwtUtil.isTokenValid(token, SIGNING_KEY, JwtUtil.TOKEN_TYPE_ACCESS, 10001L));
        assertThrows(ExpiredJwtException.class, () -> JwtUtil.parseSignedClaims(token, SIGNING_KEY));
    }

    @Test
    void shouldRejectShortSecret() {
        assertThrows(IllegalArgumentException.class, () -> JwtUtil.createHmacShaKey("too-short-secret"));
    }

    @Test
    void getBase64Secret() {
        String rawSecret = "abcdefghijklmnopqrstuvwxyz123456";
        System.out.println(Base64.getEncoder().encodeToString(rawSecret.getBytes(StandardCharsets.UTF_8)));   // YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=
    }
}
