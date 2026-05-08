package com.ccsanjuu.blog.config;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.modules.auth.constants.AuthConstants;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

/**
 * 从请求头中解析 Access Token，并把认证结果写入 Spring Security 上下文。
 *
 * <p>这个过滤器只处理当前项目里明确需要登录态的接口。公开接口即使带了一个过期 Token，
 * 也不会被这里拦截，避免影响游客浏览公开内容。</p>
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_PREFIX = "/api/v1";
    private static final String ADMIN_API_PREFIX = API_PREFIX + "/admin/";
    private static final String CURRENT_USER_API = API_PREFIX + "/users/me";
    private static final String CURRENT_USER_API_PREFIX = CURRENT_USER_API + "/";

    private final SecretKey jwtSigningKey;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !requiresAccessToken(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);

        if (authorization == null || authorization.isBlank()) {
            // 没有携带 Token 时交给后续的 .authenticated() / hasRole(...) 判断。
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith(AuthConstants.BEARER_TOKEN_PREFIX)) {
            writeErrorResponse(response, ResultCode.ACCESS_TOKEN_INVALID);
            return;
        }

        String accessToken = authorization.substring(AuthConstants.BEARER_TOKEN_PREFIX.length());

        try {
            Claims claims = JwtUtil.parseClaims(accessToken, jwtSigningKey);
            if (!JwtUtil.TOKEN_TYPE_ACCESS.equals(claims.get(JwtUtil.CLAIM_TOKEN_TYPE, String.class))) {
                writeErrorResponse(response, ResultCode.ACCESS_TOKEN_INVALID);
                return;
            }

            JwtPrincipal principal = buildPrincipal(claims);
            List<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + principal.role())
            );

            /*
             * UsernamePasswordAuthenticationToken 是 Spring Security 的一种 Authentication 实现。
             * 这里不是在做用户名密码登录，而是借用它承载“当前请求已经认证通过”的结果。
             * 后续 .authenticated() 会判断 SecurityContext 中是否存在已认证的 Authentication；
             * hasRole("ADMIN") 会判断 authorities 中是否存在 ROLE_ADMIN。
             */
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);   // 🔺之后要获取当前请求的用户的 userId、username、role、status，就可以通过 Security ContextHolder.getContext().getAuthentication().getPrincipal() 拿到这个 JwtPrincipal 了。

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            writeErrorResponse(response, ResultCode.ACCESS_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            writeErrorResponse(response, ResultCode.ACCESS_TOKEN_INVALID);
        }
    }

    private boolean requiresAccessToken(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith(ADMIN_API_PREFIX)
                || path.equals(CURRENT_USER_API)
                || path.startsWith(CURRENT_USER_API_PREFIX);
    }

    private JwtPrincipal buildPrincipal(Claims claims) {
        Long userId = JwtUtil.getUserId(claims);
        String username = requireClaim(claims, JwtUtil.CLAIM_USERNAME);
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
        // 因为 JWT Filter 执行时还没进入 Controller，所以不能直接 return Result.fail(...)。如果 Token 过期或无效，我们只能通过 HttpServletResponse 手动把错误 JSON 写回前端。
    }
}
