package com.ccsanjuu.blog.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 站点级公共配置。
 */
@Data
@ConfigurationProperties(prefix = "blog")
public class BlogProperties {

    private String appName = "青禾边";

    /**
     * 获取邮件和其他站点功能使用的应用名称。
     *
     * @return 应用名称
     */
    public String getAppName() {
        return appName == null || appName.isBlank() ? "青禾边" : appName.trim();
    }
}
