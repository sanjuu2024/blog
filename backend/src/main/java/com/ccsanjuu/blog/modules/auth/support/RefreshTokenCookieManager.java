package com.ccsanjuu.blog.modules.auth.support;

import com.ccsanjuu.blog.properties.RefreshTokenCookieProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieManager {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/v1/auth";
    private final RefreshTokenCookieProperties refreshTokenCookieProperties;

    /**
     * 让浏览器设置 RT Cookie
     * @param response
     * @param refreshToken
     * @param expiresAt
     */
    public void addRefreshTokenCookie(
            HttpServletResponse response,
            String refreshToken,
            OffsetDateTime expiresAt
    ) {
        Duration maxAge = Duration.between(OffsetDateTime.now(ZoneOffset.UTC), expiresAt);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(refreshTokenCookieProperties.secure()) // 生产环境 HTTPS 下应为 true
                .sameSite("Lax")
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 清除浏览器端 Cookie 中的 RT
     * @param response
     */
    // 浏览器删除 Cookie 的本质不是“发一个删除命令”，而是“再设置一个同名、同 Path、同 Domain、立即过期的 Cookie”
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshTokenCookieProperties.secure())
                .sameSite("Lax")
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
