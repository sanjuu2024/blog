package com.ccsanjuu.blog.config;

import com.ccsanjuu.blog.properties.TurnstileProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(TurnstileProperties.class)
public class TurnstileConfig {

    /**
     * 校验启用 Turnstile 时所需的配置，生产环境额外要求绑定 hostname。
     *
     * @param properties Turnstile 配置
     * @param environment 当前运行环境
     */
    public TurnstileConfig(TurnstileProperties properties, Environment environment) {
        if (!properties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(properties.getSecretKey())
                || (environment.acceptsProfiles(Profiles.of("prod"))
                && (!StringUtils.hasText(properties.getExpectedHostname())
                || !StringUtils.hasText(properties.getExpectedAction())))) {
            throw new IllegalStateException("Turnstile 配置不完整");
        }
    }

    /**
     * 创建 Turnstile Siteverify 专用客户端，限制外部验证请求占用业务线程的时间。
     *
     * @param builder RestClient 构建器
     * @return Turnstile HTTP 客户端
     */
    @Bean
    public RestClient turnstileRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return builder
                .baseUrl("https://challenges.cloudflare.com")
                .requestFactory(requestFactory)
                .build();
    }
}
