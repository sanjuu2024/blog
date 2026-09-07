package com.ccsanjuu.blog.config;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class SecurityConfig {

    /**
     * 这些路径无需登录。（即不要求携带有效 Access Token）。
     *
     * <p>特别注意：/auth/refresh 是用 Refresh Token 换新 Token 的入口，
     * 调用它时 Access Token 往往已经过期，所以这里必须放行给 service 层自己校验 RT。</p>
     */
    private static final String[] AUTH_WHITELIST = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            // knife4j（否则访问会弹出一个登录界面）
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    // 不用启用 CORS，后续前端开发会配置 vite 服务器代理的
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            SecretKey jwtSigningKey,
            TokenVersionService tokenVersionService
    ) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtSigningKey, objectMapper, tokenVersionService);

        http
                // 前后端分离 + JWT 场景下不依赖浏览器 Cookie Session，所以关闭 CSRF。
                .csrf(AbstractHttpConfigurer::disable)
                // 后端不创建 HTTP Session，每次请求都通过 Access Token 恢复身份。
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, exception) -> {
                            log.warn(
                                    "security_event=AUTHENTICATION_REQUIRED description=\"需要登录才能访问\" outcome=FAIL method={} path={}",
                                    request.getMethod(),
                                    request.getRequestURI()
                            );
                            writeErrorResponse(response, objectMapper, ResultCode.ACCESS_TOKEN_INVALID);
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            log.warn(
                                    "security_event=ACCESS_DENIED description=\"无权限访问\" outcome=FAIL method={} path={}",
                                    request.getMethod(),
                                    request.getRequestURI()
                            );
                            writeErrorResponse(response, objectMapper, ResultCode.NO_PERMISSION);
                        })
                )
                // 🔺先解析 Bearer Access Token，下面代码再让 authenticated()/hasRole(...) 做鉴权判断。
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AUTH_WHITELIST).permitAll()   // 白名单放行
                        // 管理端接口只允许管理员访问
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // 个人中心相关接口要求普通登录态即可访问
                        .requestMatchers("/api/v1/users/me", "/api/v1/users/me/**").authenticated()
                        // 前台评论写入和删除要求登录
                        .requestMatchers(HttpMethod.POST, "/api/v1/articles/*/comments").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/comments/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/messages/*").authenticated()
                        // 其余接口先按公开访问处理，后续业务逐步实现再补充完善
                        .anyRequest().permitAll()
                )
                // 当前项目不使用默认登录页和 HTTP Basic 弹窗认证
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 加盐哈希
        return new BCryptPasswordEncoder();
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            ResultCode resultCode
    ) throws IOException {
        response.setStatus(resultCode.getHttpStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.fail(resultCode));
    }
}
