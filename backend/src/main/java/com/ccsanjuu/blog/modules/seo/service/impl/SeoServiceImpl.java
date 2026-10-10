package com.ccsanjuu.blog.modules.seo.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.ccsanjuu.blog.modules.seo.model.vo.SeoMetadataVO;
import com.ccsanjuu.blog.modules.seo.service.SeoService;
import com.ccsanjuu.blog.properties.BlogProperties;
import com.ccsanjuu.blog.properties.SeoProperties;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeoServiceImpl implements SeoService {

    private static final Map<String, String> PUBLIC_PAGES = Map.of(
            "/", "首页", "/articles", "文章", "/about", "关于", "/messages", "留言板");

    private final ArticleService articleService;
    private final ArticleMapper articleMapper;
    private final BlogProperties blogProperties;
    private final SeoProperties seoProperties;
    private final ResourceLoader resourceLoader;

    /**
     * 读取公开页面元信息；使用无身份的文章查询，避免 SEO 请求计数或返回个人状态。
     *
     * @param path 页面规范路径
     * @return 元信息
     */
    @Override
    public SeoMetadataVO getMetadata(String path) {
        if (path.matches("/articles/[1-9][0-9]*")) {
            long articleId;
            try {
                articleId = Long.parseLong(path.substring("/articles/".length()));
            } catch (NumberFormatException exception) {
                throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
            }
            try {
                // 纯读取详情已包含同源 SEO，不另查文章或产生浏览统计。
                return articleService.getPublicArticleDetail(articleId, null, null).getSeo();
            } catch (BizException exception) {
                // SEO 入口统一视为页面不存在，分类禁用等内部业务状态不改变公开页面的 404 语义。
                throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
            }
        }
        if (!PUBLIC_PAGES.containsKey(path)) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
        }
        String title = PUBLIC_PAGES.get(path);
        return SeoMetadataVO.builder()
                .canonicalUrl(baseUrl() + path).type("website")
                .title("/".equals(path) ? blogProperties.getAppName() : title + " - " + blogProperties.getAppName())
                .description(blogProperties.getAppName() + "的" + title)
                .build();
    }

    /**
     * 使用当前构建的入口模板渲染文章和页面信息，保留原有 Vue 资源标签。
     *
     * @param path 页面规范路径
     * @return 首屏 HTML
     */
    @Override
    public String renderHtml(String path) {
        SeoMetadataVO metadata = getMetadata(path);
        Document document;
        // 每次读取模板，使前端更新后的带哈希资源路径立即生效。
        try (var input = resourceLoader.getResource(seoProperties.getHtmlTemplate()).getInputStream()) {
            document = Jsoup.parse(input, StandardCharsets.UTF_8.name(), baseUrl());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "前端入口模板不可用", exception);
        }
        Element app = document.getElementById("app");
        if (app == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "前端入口模板缺少 app 容器");
        }
        document.select("html").attr("lang", "zh-CN");
        document.title(metadata.getTitle());
        document.head().select("meta[name=description], meta[name=robots], link[rel=canonical], meta[property^=og:], meta[property^=article:]").remove();
        document.head().appendElement("link").attr("rel", "canonical")
                .attr("href", metadata.getCanonicalUrl()).attr("data-seo", "true");
        addMeta(document, "name", "description", metadata.getDescription());
        addMeta(document, "name", "robots", "index, follow");
        addMeta(document, "property", "og:title", metadata.getTitle());
        addMeta(document, "property", "og:description", metadata.getDescription());
        addMeta(document, "property", "og:url", metadata.getCanonicalUrl());
        addMeta(document, "property", "og:type", metadata.getType());
        addMeta(document, "property", "og:site_name", blogProperties.getAppName());
        addMeta(document, "property", "og:locale", "zh_CN");
        addMeta(document, "property", "og:image", metadata.getImageUrl());
        addMeta(document, "property", "article:published_time", metadata.getPublishedAt());
        addMeta(document, "property", "article:modified_time", metadata.getUpdatedAt());
        app.empty();
        return document.outerHtml();
    }

    /**
     * 实时输出可见文章地址，下线或禁用分类后不再进入 sitemap。
     *
     * @return 站点地图
     */
    @Override
    public String getSitemap() {
        try {
            StringWriter output = new StringWriter();
            XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("urlset");
            xml.writeDefaultNamespace("http://www.sitemaps.org/schemas/sitemap/0.9");
            for (String path : PUBLIC_PAGES.keySet().stream().sorted().toList()) {
                writeSitemapUrl(xml, path, null);
            }
            for (var article : articleMapper.selectPublicSeoArticles()) {
                writeSitemapUrl(xml, "/articles/" + article.getId(), article.getUpdatedAt());
            }
            xml.writeEndElement();
            xml.writeEndDocument();
            xml.close();
            return output.toString();
        } catch (XMLStreamException exception) {
            throw new IllegalStateException("站点地图生成失败", exception);
        }
    }

    /**
     * 输出站点抓取限制，权限控制仍由 Spring Security 负责。
     *
     * @return robots 文本
     */
    @Override
    public String getRobots() {
        // Vue 正文依赖公开 API，允许加载这些数据；JSON 本身由 Nginx 的 noindex 响应头禁止收录。
        return "User-agent: *\nDisallow: /admin\nDisallow: /auth\nDisallow: /user\nDisallow: /api/\n"
                + "Allow: /api/v1/articles\nAllow: /api/v1/comments/\nAllow: /api/v1/categories\n"
                + "Allow: /api/v1/tags\nAllow: /api/v1/about\nAllow: /api/v1/messages\n"
                + "Allow: /api/v1/seo\nAllow: /api/v1/users/*/public-profile\n"
                + "Sitemap: " + baseUrl() + "/sitemap.xml\n";
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
     * 输出标签属性，由 Jsoup 处理标题、描述中的 HTML 特殊字符。
     *
     * @param document 页面文档
     * @param attribute 元信息属性名
     * @param key 元信息键
     * @param value 元信息值，为空时不输出
     */
    private void addMeta(Document document, String attribute, String key, Object value) {
        if (value != null) {
            document.head().appendElement("meta").attr(attribute, key)
                    .attr("content", value.toString()).attr("data-seo", "true");
        }
    }

    /**
     * 输出单个 sitemap 地址，由 XML writer 转义 URL。
     *
     * @param xml XML writer
     * @param path 规范路径
     * @param updatedAt 内容更新时间
     * @throws XMLStreamException XML 写入失败
     */
    private void writeSitemapUrl(XMLStreamWriter xml, String path, OffsetDateTime updatedAt) throws XMLStreamException {
        xml.writeStartElement("url");
        xml.writeStartElement("loc");
        xml.writeCharacters(baseUrl() + path);
        xml.writeEndElement();
        if (updatedAt != null) {
            xml.writeStartElement("lastmod");
            xml.writeCharacters(updatedAt.toString());
            xml.writeEndElement();
        }
        xml.writeEndElement();
    }

}
