package com.ccsanjuu.blog.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.AssertTrue;
import java.net.URI;

/**
 * 站点级公共配置。
 */
@Data
@ConfigurationProperties(prefix = "blog")
@Validated
public class BlogProperties {

    private String appName;

    private URI siteUrl;

    /**
     * 校验站点地址仅包含 HTTP(S) 协议、主机和可选端口，避免错误 canonical 或凭据泄露。
     *
     * @return 是否为有效的站点 origin
     */
    @AssertTrue(message = "blog.site-url 必须是无路径、查询、片段或凭据的 HTTP(S) 站点地址")
    public boolean isSiteUrlValid() {
        return siteUrl != null && ("http".equalsIgnoreCase(siteUrl.getScheme())
                || "https".equalsIgnoreCase(siteUrl.getScheme()))
                && siteUrl.getHost() != null && siteUrl.getUserInfo() == null
                && siteUrl.getQuery() == null && siteUrl.getFragment() == null
                && (siteUrl.getPath().isEmpty() || "/".equals(siteUrl.getPath()));
    }

    /**
     * 获取邮件和其他站点功能使用的应用名称。
     *
     * @return 应用名称
     */
    public String getAppName() {
        return appName == null || appName.isBlank() ? "青禾边" : appName.trim();
    }
}
