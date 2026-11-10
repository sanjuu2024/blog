package com.ccsanjuu.blog.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "blog.turnstile")
public class TurnstileProperties {

    private boolean enabled;

    private String secretKey;

    private String expectedHostname;

    private String expectedAction;
}
