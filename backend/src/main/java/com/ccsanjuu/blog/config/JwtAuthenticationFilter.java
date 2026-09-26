package com.ccsanjuu.blog.config;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.modules.auth.constants.AuthConstants;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 从请求头中解析 Access Token，并把认证结果写入 Spring Security 上下文。
 *
 * <p>普通公开内容接口即使带了一个过期 Token，也可以按游客身份继续访问。
 * 但评论和留言列表会根据登录身份改变数据可见性，留言发表还会使用不同的请求字段；
 * 这些接口一旦携带 Token 就必须校验成功，避免已登录用户被静默降级成游客。</p>
 */
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_PREFIX = "/api/v1";
    private static final String ADMIN_API_PREFIX = API_PREFIX + "/admin/";
    private static final String CURRENT_USER_API = API_PREFIX + "/users/me";
    private static final String CURRENT_USER_API_PREFIX = CURRENT_USER_API + "/";
    private static final String MESSAGE_API = API_PREFIX + "/messages";
    private static final Pattern ARTICLE_COMMENT_API_PATTERN = Pattern.compile("^" + API_PREFIX + "/articles/[^/]+/comments$");
    private static final Pattern ARTICLE_DETAIL_API_PATTERN = Pattern.compile("^" + API_PREFIX + "/articles/[^/]+$");
    private static final Pattern COMMENT_DETAIL_API_PATTERN = Pattern.compile("^" + API_PREFIX + "/comments/[^/]+$");
    private static final Pattern COMMENT_REPLIES_API_PATTERN = Pattern.compile("^" + API_PREFIX + "/comments/[^/]+/replies$");
    private static final Pattern MESSAGE_DETAIL_API_PATTERN = Pattern.compile("^" + MESSAGE_API + "/[^/]+$");

    private final SecretKey jwtSigningKey;
    private final ObjectMapper objectMapper;
    private final TokenVersionService tokenVersionService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !mustRejectInvalidAccessToken(request) && !hasAuthorizationHeader(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        boolean invalidAccessTokenMustBeRejected = mustRejectInvalidAccessToken(request);
        String authorization = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);

        if (authorization == null || authorization.isBlank()) {
            // 没有携带 Token 时交给后续权限规则或允许匿名访问的 Controller 处理。
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith(AuthConstants.BEARER_TOKEN_PREFIX)) {
            rejectRequestOrContinue(
                    invalidAccessTokenMustBeRejected,
                    request,
                    response,
                    filterChain,
                    ResultCode.ACCESS_TOKEN_INVALID,
                    "INVALID_AUTHORIZATION_FORMAT"
            );
            return;
        }

        String accessToken = authorization.substring(AuthConstants.BEARER_TOKEN_PREFIX.length());
        Claims claims;
        String tokenType;

        try {
            claims = JwtUtil.parseClaims(accessToken, jwtSigningKey);
            tokenType = claims.get(JwtUtil.CLAIM_TOKEN_TYPE, String.class);
        } catch (ExpiredJwtException ex) {
            rejectRequestOrContinue(
                    invalidAccessTokenMustBeRejected,
                    request,
                    response,
                    filterChain,
                    ResultCode.ACCESS_TOKEN_EXPIRED,
                    "EXPIRED"
            );
            return;
        } catch (JwtException | IllegalArgumentException ex) {
            rejectRequestOrContinue(
                    invalidAccessTokenMustBeRejected,
                    request,
                    response,
                    filterChain,
                    ResultCode.ACCESS_TOKEN_INVALID,
                    "INVALID_TOKEN"
            );
            return;
        }

        if (!JwtUtil.TOKEN_TYPE_ACCESS.equals(tokenType)) {
            rejectRequestOrContinue(
                    invalidAccessTokenMustBeRejected,
                    request,
                    response,
                    filterChain,
                    ResultCode.ACCESS_TOKEN_INVALID,
                    "INVALID_TOKEN_TYPE"
            );
            return;
        }

        Long userId;
        Long tokenVersion;
        JwtPrincipal principal;
        try {
            userId = JwtUtil.getUserId(claims);
            tokenVersion = JwtUtil.getTokenVersion(claims);
            principal = buildPrincipal(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            rejectRequestOrContinue(
                    invalidAccessTokenMustBeRejected,
                    request,
                    response,
                    filterChain,
                    ResultCode.ACCESS_TOKEN_INVALID,
                    "INVALID_TOKEN"
            );
            return;
        }

        // 每次请求比较 Redis/数据库中的当前版本，令密码、状态或角色变化后的旧 Token 立即失效。
        Long currentVersion = tokenVersionService.getCurrentVersion(userId);
        if (currentVersion == null || !currentVersion.equals(tokenVersion)) {
            rejectRequestOrContinue(
                    invalidAccessTokenMustBeRejected,
                    request,
                    response,
                    filterChain,
                    ResultCode.ACCESS_TOKEN_INVALID,
                    "TOKEN_VERSION_MISMATCH"
            );
            return;
        }

        // Spring Security 的角色判断看的是 authorities，不会直接读取 JwtPrincipal.role()。
        // hasRole("ADMIN") 实际会检查这里是否存在 "ROLE_ADMIN"；所以项目角色 ADMIN 需要转换成 ROLE_ADMIN。
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + principal.role())
        );

        /*
         * UsernamePasswordAuthenticationToken 是 Spring Security 的一种 Authentication 实现。
         * 这里不是重新做“用户名 + 密码”登录，而是在 JWT 已经验签通过后，
         * 创建一个 authenticated=true 的 Authentication，告诉 Spring Security：
         * “当前请求已经有合法身份，用户信息是 principal，权限列表是 authorities。”
         *
         * 三个参数分别是：
         * - principal：当前登录用户信息，本项目里是 JwtPrincipal(userId, username, role, status)
         * - credentials：登录凭证；JWT 已经验证完了，不需要再保存密码或 Token，所以传 null
         * - authorities：当前用户权限；后续 hasRole("ADMIN") 会检查这里是否存在 ROLE_ADMIN
         */
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        // 把认证结果放入当前请求线程的 SecurityContext。
        // 之后 Controller 可以通过 @AuthenticationPrincipal JwtPrincipal principal 直接拿到当前用户。
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // JWT 异常处理已经结束，下游业务异常应继续交给项目统一异常处理器。
        filterChain.doFilter(request, response);
    }

    private void rejectRequestOrContinue(
            boolean invalidAccessTokenMustBeRejected,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            ResultCode resultCode,
            String reason
    ) throws IOException, ServletException {
        if (invalidAccessTokenMustBeRejected) {
            rejectRequest(request, response, resultCode, reason);
            return;
        }
        log.warn(
                "security_event=OPTIONAL_ACCESS_TOKEN_IGNORED description=\"公开接口携带的 Access Token 无效，按游客身份继续访问\" outcome=CONTINUE reason={} method={} path={}",
                reason,
                request.getMethod(),
                request.getRequestURI()
        );
        filterChain.doFilter(request, response);
    }

    private void rejectRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            ResultCode resultCode,
            String reason
    ) throws IOException {
        log.warn(
                "security_event=ACCESS_TOKEN_REJECTED description=\"Access Token 被拒绝\" outcome=FAIL reason={} method={} path={}",
                reason,
                request.getMethod(),
                request.getRequestURI()
        );
        writeErrorResponse(response, resultCode);
    }

    private boolean mustRejectInvalidAccessToken(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith(ADMIN_API_PREFIX)
                || path.equals(CURRENT_USER_API)
                || path.startsWith(CURRENT_USER_API_PREFIX)
                || ("GET".equals(request.getMethod()) && ARTICLE_DETAIL_API_PATTERN.matcher(path).matches())
                || isIdentitySensitiveCommentApi(request, path)
                || isIdentitySensitiveMessageApi(request, path);
    }

    private boolean hasAuthorizationHeader(HttpServletRequest request) {
        String authorization = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        return authorization != null && !authorization.isBlank();
    }

    /**
     * 评论读取接口允许游客访问，但携带 Token 时必须保证身份有效，才能正确返回本人非公开评论。
     * 评论写入和删除接口本身要求登录，这里同时保证过期 Token 返回准确的鉴权错误。
     *
     * @param request HTTP 请求
     * @param path 请求路径
     * @return 是否需要拒绝无效的已携带 Token
     */
    private boolean isIdentitySensitiveCommentApi(HttpServletRequest request, String path) {
        String method = request.getMethod();
        return (ARTICLE_COMMENT_API_PATTERN.matcher(path).matches()
                && ("GET".equals(method) || "POST".equals(method)))
                || ("GET".equals(method) && COMMENT_REPLIES_API_PATTERN.matcher(path).matches())
                || ("DELETE".equals(method) && COMMENT_DETAIL_API_PATTERN.matcher(path).matches());
    }

    /**
     * 留言列表和发表接口允许不携带 Token 的游客访问，但携带 Token 时必须保证身份有效。
     * 删除接口本身要求登录，这里同时保证过期 Token 返回准确的鉴权错误。
     *
     * @param request HTTP 请求
     * @param path 请求路径
     * @return 是否需要拒绝无效的已携带 Token
     */
    private boolean isIdentitySensitiveMessageApi(HttpServletRequest request, String path) {
        String method = request.getMethod();
        return (MESSAGE_API.equals(path) && ("GET".equals(method) || "POST".equals(method)))
                || ("DELETE".equals(method) && MESSAGE_DETAIL_API_PATTERN.matcher(path).matches());
    }

    private JwtPrincipal buildPrincipal(Claims claims) {
        Long userId = JwtUtil.getUserId(claims);
        String username = requireClaim(claims, JwtUtil.CLAIM_USERNAME);
        // role 是登录生成 Access Token 时写入的 claim，来源于数据库中的 blog_user.role。
        String role = requireClaim(claims, JwtUtil.CLAIM_ROLE);
        String status = requireClaim(claims, JwtUtil.CLAIM_STATUS);
        return new JwtPrincipal(userId, username, role, status);
    }

    private String requireClaim(Claims claims, String claimName) {
        String value = claims.get(claimName, String.class);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JWT claim is missing: " + claimName);
        }
        return value;
    }

    private void writeErrorResponse(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setStatus(resultCode.getHttpStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.fail(resultCode));
        // 因为 JWT Filter 执行时还没进入 Controller，所以不能直接 return Result.fail(...)。如果 Token 过期或无效，则只能通过 HttpServletResponse 手动把错误 JSON 写回前端。
    }
}
