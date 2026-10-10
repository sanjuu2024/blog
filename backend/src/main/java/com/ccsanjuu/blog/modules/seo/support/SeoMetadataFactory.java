package com.ccsanjuu.blog.modules.seo.support;

import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.seo.model.vo.SeoMetadataVO;
import com.ccsanjuu.blog.properties.BlogProperties;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.net.URI;

/** 首屏 HTML 与文章详情共用的纯元信息生成逻辑，不查询数据库。 */
@Component
@RequiredArgsConstructor
public class SeoMetadataFactory {

    private final BlogProperties blogProperties;

    /**
     * 从已经查询的文章生成元信息，不再次读取正文或用户状态。
     *
     * @param article 已验证可见性的文章
     * @return 文章元信息
     */
    public SeoMetadataVO fromArticle(Article article) {
        String summary = article.getSummary() == null ? "" : Jsoup.parse(article.getSummary()).text();
        if (summary.isBlank()) {
            summary = Jsoup.parse(article.getContentHtml()).text();
        }
        return SeoMetadataVO.builder()
                .title(article.getTitle() + " - " + blogProperties.getAppName())
                .description(summary.substring(0, Math.min(summary.length(), 160)))
                .canonicalUrl(baseUrl() + "/articles/" + article.getId())
                .imageUrl(imageUrl(article.getCoverUrl()))
                .type("article")
                .publishedAt(article.getPublishedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    /**
     * 获取配置的站点 origin，不使用请求 Host 构建公开地址。
     *
     * @return 无末尾斜杠的站点地址
     */
    private String baseUrl() {
        return blogProperties.getSiteUrl().toString().replaceAll("/$", "");
    }

    /**
     * 将封面转为 HTTP(S) 绝对地址，不输出脚本协议或错误 URL。
     *
     * @param image 封面地址
     * @return 绝对地址，无封面或地址无效时为空
     */
    private String imageUrl(String image) {
        if (image == null || image.isBlank()) {
            return null;
        }
        try {
            URI url = URI.create(baseUrl() + "/").resolve(image);
            return ("https".equalsIgnoreCase(url.getScheme()) || "http".equalsIgnoreCase(url.getScheme()))
                    && url.getHost() != null && url.getUserInfo() == null ? url.toString() : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
