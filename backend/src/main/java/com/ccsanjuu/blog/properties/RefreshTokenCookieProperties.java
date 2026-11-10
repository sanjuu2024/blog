package com.ccsanjuu.blog.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog.auth.refresh-cookie")
public record RefreshTokenCookieProperties(
    boolean secure
){}
