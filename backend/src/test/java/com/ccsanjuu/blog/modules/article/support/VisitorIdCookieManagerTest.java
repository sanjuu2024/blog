package com.ccsanjuu.blog.modules.article.support;

import com.ccsanjuu.blog.properties.RefreshTokenCookieProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitorIdCookieManagerTest {

    @Test
    void shouldCreateSecureHttpOnlyVisitorCookieWhenMissing() {
        VisitorIdCookieManager manager = new VisitorIdCookieManager(
                new RefreshTokenCookieProperties(true)
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        String token = manager.resolve(new MockHttpServletRequest(), response);
        String cookie = response.getHeader("Set-Cookie");

        assertEquals(36, token.length());
        assertTrue(cookie.contains("visitor_id=" + token));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Path=/"));
    }

    @Test
    void shouldReuseExistingVisitorCookie() {
        VisitorIdCookieManager manager = new VisitorIdCookieManager(
                new RefreshTokenCookieProperties(false)
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("visitor_id", "existing-visitor-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertEquals("existing-visitor-token", manager.resolve(request, response));
        assertEquals(null, response.getHeader("Set-Cookie"));
    }
}
