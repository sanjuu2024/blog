package com.ccsanjuu.blog.modules.auth.model.security;

/**
 * Access Token 解析出来的当前登录用户快照。
 *
 * <p>它会作为 Authentication.principal 放入 Spring Security 上下文，后续业务代码如果需要当前用户 ID，
 * 可以从 SecurityContext 或 @AuthenticationPrincipal 中取得。</p>
 */
public record JwtPrincipal(
        Long userId,
        String username,
        String role,
        String status
) {
}
