package com.ccsanjuu.blog.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;

/**
 * 站点级公共配置。
 */
@Data
@ConfigurationProperties(prefix = "blog")
@Validated
public class BlogProperties {

    @NotBlank(message = "BLOG_APP_NAME 不能为空")
    private String appName = "";

    private URI siteUrl;

    @NotBlank(message = "BLOG_DESC 不能为空")
    private String description = "";

    @NotBlank(message = "BLOG_LOGO_URL 不能为空")
    private String logoUrl = "";

    private boolean requireHttpsSiteUrl;

    private boolean requireHttpsLogoUrl;

    /**
     * 校验站点地址为有效 origin；生产环境要求 HTTPS，避免错误 canonical 或凭据泄露。
     *
     * @return 是否为有效的站点 origin
     */
    @AssertTrue(message = "BLOG_SITE_URL 必须是无路径、查询、片段或凭据的站点 origin，生产环境必须使用 HTTPS")
    public boolean isSiteUrlValid() {
        boolean https = siteUrl != null && "https".equalsIgnoreCase(siteUrl.getScheme());
        boolean allowedHttp = !requireHttpsSiteUrl
                && siteUrl != null && "http".equalsIgnoreCase(siteUrl.getScheme());
        return siteUrl != null && (https || allowedHttp)
                && siteUrl.getHost() != null && siteUrl.getUserInfo() == null
                && siteUrl.getQuery() == null && siteUrl.getFragment() == null
                && (siteUrl.getPath().isEmpty() || "/".equals(siteUrl.getPath()));
    }

    /**
     * 校验站点 Logo 地址为无凭据的 HTTP(S) 绝对 URL；生产环境要求 HTTPS。
     *
     * @return Logo 地址是否合法
     */
    @AssertTrue(message = "BLOG_LOGO_URL 必须是无凭据的绝对 URL，生产环境必须使用 HTTPS")
    public boolean isLogoUrlValid() {
        if (logoUrl == null || logoUrl.isBlank()) {
            return true;
        }
        try {
            URI uri = URI.create(logoUrl.trim());
            boolean https = "https".equalsIgnoreCase(uri.getScheme());
            boolean allowedHttp = !requireHttpsLogoUrl && "http".equalsIgnoreCase(uri.getScheme());
            return (https || allowedHttp)
                    && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * 获取去除首尾空白的应用名称。
     *
     * @return 应用名称
     */
    public String getAppName() {
        return appName == null ? "" : appName.trim();
    }
}
