package com.ccsanjuu.blog.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "blog.mail")
public class BlogMailProperties {

    private boolean enabled;

    private String from;

    private String frontendBaseUrl;

    /**
     * 获取用于生成邮件链接的前端根地址，并移除末尾斜杠。
     *
     * @return 规范化后的前端根地址
     */
    public String getFrontendBaseUrl() {
        if (frontendBaseUrl == null) {
            return null;
        }
        return frontendBaseUrl.trim().replaceAll("/+$", "");
    }

}
