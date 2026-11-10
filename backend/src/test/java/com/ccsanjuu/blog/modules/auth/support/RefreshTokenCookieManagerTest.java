package com.ccsanjuu.blog.modules.auth.support;

import com.ccsanjuu.blog.properties.RefreshTokenCookieProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenCookieManagerTest {

    @Test
    void addRefreshTokenCookieShouldUseSecureCookieWhenSecureIsEnabled() {
        RefreshTokenCookieManager cookieManager =
                new RefreshTokenCookieManager(new RefreshTokenCookieProperties(true));
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieManager.addRefreshTokenCookie(
                response,
                "refresh-token-value",
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(7)
        );

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.startsWith("refresh_token=refresh-token-value;"));
        assertTrue(setCookie.contains("Path=/api/v1/auth"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("SameSite=Lax"));
        assertTrue(setCookie.contains("Max-Age="));
        assertFalse(setCookie.contains("Max-Age=0"));
    }

    @Test
    void addRefreshTokenCookieShouldNotUseSecureCookieWhenSecureIsDisabled() {
        RefreshTokenCookieManager cookieManager =
                new RefreshTokenCookieManager(new RefreshTokenCookieProperties(false));
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieManager.addRefreshTokenCookie(
                response,
                "refresh-token-value",
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(7)
        );

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertFalse(setCookie.contains("Secure"));
    }

    @Test
    void clearRefreshTokenCookieShouldExpireMatchingCookie() {
        RefreshTokenCookieManager cookieManager =
                new RefreshTokenCookieManager(new RefreshTokenCookieProperties(true));
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieManager.clearRefreshTokenCookie(response);

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.startsWith("refresh_token=;"));
        assertTrue(setCookie.contains("Path=/api/v1/auth"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("SameSite=Lax"));
        assertTrue(setCookie.contains("Max-Age=0"));
    }
}
