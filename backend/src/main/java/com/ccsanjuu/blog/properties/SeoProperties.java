package com.ccsanjuu.blog.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** SEO 首屏 HTML 使用的前端入口模板配置。 */
@Data
@ConfigurationProperties(prefix = "blog.seo")
public class SeoProperties {

    private String htmlTemplate;
}
