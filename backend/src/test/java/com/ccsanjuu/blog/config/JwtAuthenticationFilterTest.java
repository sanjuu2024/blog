package com.ccsanjuu.blog.config;

import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class JwtAuthenticationFilterTest {

    private static final SecretKey SIGNING_KEY =
            JwtUtil.createHmacShaKey("0123456789abcdef0123456789abcdef");

    private final TokenVersionService tokenVersionService = mock(TokenVersionService.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(SIGNING_KEY, new ObjectMapper(), tokenVersionService);

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

    @Test
    void shouldAcceptAccessTokenWhenTokenVersionMatches() throws Exception {
        when(tokenVersionService.getCurrentVersion(anyLong())).thenReturn(0L);
        String accessToken = JwtUtil.generateAccessToken(
                SIGNING_KEY,
                "sanjuu-blog",
                Duration.ofMinutes(15),
                10001L,
                "sanjuu",
                "USER",
                "ACTIVE",
                0L
        );

        MockHttpServletResponse response = performProtectedRequest(accessToken);

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldRejectAccessTokenWhenTokenVersionDoesNotMatch(CapturedOutput output) throws Exception {
        when(tokenVersionService.getCurrentVersion(anyLong())).thenReturn(1L);
        String accessToken = JwtUtil.generateAccessToken(
                SIGNING_KEY,
                "sanjuu-blog",
                Duration.ofMinutes(15),
                10001L,
                "sanjuu",
                "USER",
                "ACTIVE",
                0L
        );

        MockHttpServletResponse response = performProtectedRequest(accessToken);

        assertEquals(401, response.getStatus());
        assertTrue(output.getOut().contains("reason=TOKEN_VERSION_MISMATCH"));
    }

    @Test
    void shouldNotTreatDownstreamBusinessExceptionAsInvalidToken() {
        when(tokenVersionService.getCurrentVersion(anyLong())).thenReturn(0L);
        String accessToken = JwtUtil.generateAccessToken(
                SIGNING_KEY,
                "sanjuu-blog",
                Duration.ofMinutes(15),
                10001L,
                "sanjuu",
                "USER",
                "ACTIVE",
                0L
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader("Authorization", "Bearer " + accessToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                    throw new IllegalArgumentException("business validation failed");
                })
        );

        assertEquals("business validation failed", exception.getMessage());
        assertEquals(200, response.getStatus());
    }

    @Test
    void anonymousMessageRequestShouldContinueWithoutAccessToken() throws Exception {
        MockHttpServletResponse getResponse = performRequest("GET", "/api/v1/messages", null);
        MockHttpServletResponse postResponse = performRequest("POST", "/api/v1/messages", null);

        assertEquals(200, getResponse.getStatus());
        assertEquals(200, postResponse.getStatus());
    }

    @Test
    void articleDetailRequestShouldRejectInvalidAccessToken() throws Exception {
        MockHttpServletResponse response = performRequest(
                "GET",
                "/api/v1/articles/40001",
                "not-a-valid-jwt"
        );

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("101001"));
    }

    @Test
    void identitySensitiveMessageRequestShouldRejectInvalidAccessToken() throws Exception {
        String invalidToken = "not-a-valid-jwt";

        MockHttpServletResponse getResponse = performRequest("GET", "/api/v1/messages", invalidToken);
        MockHttpServletResponse postResponse = performRequest("POST", "/api/v1/messages", invalidToken);
        MockHttpServletResponse deleteResponse = performRequest("DELETE", "/api/v1/messages/90001", invalidToken);

        assertEquals(401, getResponse.getStatus());
        assertEquals(401, postResponse.getStatus());
        assertEquals(401, deleteResponse.getStatus());
    }

    @Test
    void anonymousCommentReadRequestShouldContinueWithoutAccessToken() throws Exception {
        MockHttpServletResponse listResponse = performRequest(
                "GET",
                "/api/v1/articles/40001/comments",
                null
        );
        MockHttpServletResponse repliesResponse = performRequest(
                "GET",
                "/api/v1/comments/60001/replies",
                null
        );

        assertEquals(200, listResponse.getStatus());
        assertEquals(200, repliesResponse.getStatus());
    }

    @Test
    void identitySensitiveCommentReadRequestShouldRejectInvalidAccessToken() throws Exception {
        String invalidToken = "not-a-valid-jwt";

        MockHttpServletResponse listResponse = performRequest(
                "GET",
                "/api/v1/articles/40001/comments",
                invalidToken
        );
        MockHttpServletResponse repliesResponse = performRequest(
                "GET",
                "/api/v1/comments/60001/replies",
                invalidToken
        );

        assertEquals(401, listResponse.getStatus());
        assertEquals(401, repliesResponse.getStatus());
    }

    @Test
    void identitySensitiveCommentReadRequestShouldRejectExpiredAccessToken() throws Exception {
        String expiredToken = Jwts.builder()
                .subject("10001")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(SIGNING_KEY)
                .compact();

        MockHttpServletResponse response = performRequest(
                "GET",
                "/api/v1/articles/40001/comments",
                expiredToken
        );

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("101002"));
    }

    private MockHttpServletResponse performProtectedRequest(String token) throws Exception {
        return performRequest("GET", "/api/v1/users/me", token);
    }

    private MockHttpServletResponse performRequest(String method, String path, String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
