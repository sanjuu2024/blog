package com.ccsanjuu.blog.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "blog.mail")
public class BlogMailProperties {

    private boolean enabled;

    private String from;

    private String frontendBaseUrl;
}
