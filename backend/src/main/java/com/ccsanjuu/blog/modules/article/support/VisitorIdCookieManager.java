package com.ccsanjuu.blog.modules.article.support;

import com.ccsanjuu.blog.properties.RefreshTokenCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VisitorIdCookieManager {

    private static final String COOKIE_NAME = "visitor_id";
    private static final String COOKIE_PATH = "/";
    private static final int MAX_TOKEN_LENGTH = 64;
    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(365);

    private final RefreshTokenCookieProperties refreshTokenCookieProperties;

    /**
     * 解析并返回当前请求对应的 visitorToken
     * - 存在则直接返回
     * - 不存在则新创建并返回；同时让浏览器设置该 cookie
     *
     * @param request
     * @param response
     * @return
     */
    public String resolve(HttpServletRequest request, HttpServletResponse response) {
        String visitorToken = findVisitorToken(request);
        if (visitorToken != null) {
            return visitorToken;
        }

        visitorToken = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, visitorToken)
                .httpOnly(true)
                .secure(refreshTokenCookieProperties.secure())   // 生产环境 HTTPS 下应为 true
                .sameSite("Lax")
                .path(COOKIE_PATH)   // 作用路径
                .maxAge(COOKIE_MAX_AGE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return visitorToken;
    }

    /**
     * 从请求对象中获取 cookie
     *
     * @param request
     * @return
     */
    private String findVisitorToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())
                    && StringUtils.hasText(cookie.getValue())
                    && cookie.getValue().length() <= MAX_TOKEN_LENGTH) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
