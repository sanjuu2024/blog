package com.ccsanjuu.blog.config;

import com.ccsanjuu.blog.common.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class JwtAuthenticationFilterTest {

    private static final SecretKey SIGNING_KEY =
            JwtUtil.createHmacShaKey("0123456789abcdef0123456789abcdef");

    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(SIGNING_KEY, new ObjectMapper());

    @Test
    void shouldLogExpiredAccessTokenWithoutExposingToken(CapturedOutput output) throws Exception {
        String expiredToken = Jwts.builder()
                .subject("10001")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(SIGNING_KEY)
                .compact();

        MockHttpServletResponse response = performProtectedRequest(expiredToken);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("101002"));
        assertTrue(output.getOut().contains("security_event=ACCESS_TOKEN_REJECTED"));
        assertTrue(output.getOut().contains("description=\"Access Token 被拒绝\""));
        assertTrue(output.getOut().contains("reason=EXPIRED"));
        assertFalse(output.getOut().contains(expiredToken));
    }

    @Test
    void shouldLogInvalidAccessTokenWithoutExposingToken(CapturedOutput output) throws Exception {
        String invalidToken = "not-a-valid-jwt";

        MockHttpServletResponse response = performProtectedRequest(invalidToken);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("101001"));
        assertTrue(output.getOut().contains("reason=INVALID_TOKEN"));
        assertFalse(output.getOut().contains(invalidToken));
    }

    @Test
    void shouldRejectRefreshTokenUsedAsAccessToken(CapturedOutput output) throws Exception {
        String refreshToken = JwtUtil.generateRefreshToken(
                SIGNING_KEY,
                "sanjuu-blog",
                Duration.ofDays(7),
                10001L,
                "refresh-jti"
        );

        MockHttpServletResponse response = performProtectedRequest(refreshToken);

        assertEquals(401, response.getStatus());
        assertTrue(output.getOut().contains("reason=INVALID_TOKEN_TYPE"));
        assertFalse(output.getOut().contains(refreshToken));
    }

    private MockHttpServletResponse performProtectedRequest(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
