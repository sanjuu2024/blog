package com.ccsanjuu.blog.config;

import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    private final JwtProperties jwtProperties;

    @Bean
    public SecretKey jwtSigningKey() {
        return JwtUtil.createHmacShaKeyFromBase64(jwtProperties.secret());   // 如果 application.yaml 用的是普通文本，那就使用 createHmacShaKey 就好
    }
}
