package com.ccsanjuu.blog.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "blog.jwt")
public record JwtProperties(
    String issuer,
    String secret,
    Duration accessTtl,
    Duration refreshTtl
){}
