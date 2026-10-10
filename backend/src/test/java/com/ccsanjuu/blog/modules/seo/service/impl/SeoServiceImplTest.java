package com.ccsanjuu.blog.modules.seo.service.impl;

import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.modules.seo.support.SeoMetadataFactory;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.vo.PublicArticleDetailVO;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.ccsanjuu.blog.properties.BlogProperties;
import com.ccsanjuu.blog.properties.SeoProperties;
import jakarta.validation.Validation;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.server.ResponseStatusException;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeoServiceImplTest {

    @Mock
    private ArticleService articleService;

    @Mock
    private ArticleMapper articleMapper;

    @TempDir
    Path directory;

    private SeoServiceImpl service;
    private SeoProperties seoProperties;
    private BlogProperties blogProperties;
    private Path template;

    @BeforeEach
    void setUp() throws Exception {
        template = directory.resolve("index.html");
        Files.writeString(template, "<!doctype html><html><head><title>站点</title></head>"
                + "<body><div id=\"app\"></div><script type=\"module\" src=\"/assets/app-123.js\"></script></body></html>");
        blogProperties = new BlogProperties();
        blogProperties.setSiteUrl(URI.create("https://blog.example.com/"));
        seoProperties = new SeoProperties();
        seoProperties.setHtmlTemplate(template.toUri().toString());
        service = new SeoServiceImpl(articleService, articleMapper,
                blogProperties, seoProperties, new DefaultResourceLoader());
    }

    @Test
    void articleHtmlShouldIncludeEscapedMetadataAndOriginalBuildAssetsWithEmptyApp() {
        when(articleService.getPublicArticleDetail(34L, null, null)).thenReturn(article());

        var html = Jsoup.parse(service.renderHtml("/articles/34"));

        assertEquals("文章 <标题> - 青禾边", html.title());
        assertEquals("https://blog.example.com/articles/34", html.selectFirst("link[rel=canonical]").attr("href"));
        assertEquals("摘要 & 描述", html.selectFirst("meta[name=description]").attr("content"));
        assertEquals("https://blog.example.com/covers/34.png", html.selectFirst("meta[property=og:image]").attr("content"));
        assertEquals("article", html.selectFirst("meta[property=og:type]").attr("content"));
        assertTrue(html.getElementById("app").html().isEmpty());
        assertTrue(html.select("main, article, nav").isEmpty());
        assertEquals("/assets/app-123.js", html.selectFirst("script").attr("src"));
        assertEquals("zh-CN", html.selectFirst("html").attr("lang"));
        assertEquals(1, html.select("link[rel=canonical]").size());
        verify(articleService).getPublicArticleDetail(34L, null, null);
        verifyNoInteractions(articleMapper);
    }

    @Test
    void invalidPathsShouldNeverExposeArticleOrPrivatePageMetadata() {
        for (String path : List.of("/admin", "/auth/login", "/articles/0", "/articles/034", "/articles/34-slug",
                "/articles/34?replyId=51", "/articles/34#article-comments", "/articles/99999999999999999999999")) {
            assertThrows(BizException.class, () -> service.getMetadata(path));
        }
        verifyNoInteractions(articleService, articleMapper);
    }

    @Test
    void disabledCategoryShouldHaveTheSame404SemanticsForHtmlAndMetadata() {
        when(articleService.getPublicArticleDetail(34L, null, null))
                .thenThrow(new BizException(ResultCode.ARTICLE_CATEGORY_DISABLED));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND,
                assertThrows(BizException.class, () -> service.getMetadata("/articles/34")).getResultCode());
        assertEquals(ResultCode.RESOURCE_NOT_FOUND,
                assertThrows(BizException.class, () -> service.renderHtml("/articles/34")).getResultCode());
    }

    @Test
    void missingSummaryShouldUseBodyAndUnsafeCoverShouldBeOmitted() {
        var article = Article.builder().id(34L).title("标题").summary("")
                .contentHtml("<p>正文内容</p>").coverUrl("javascript:alert(1)").build();
        var metadata = new SeoMetadataFactory(blogProperties).fromArticle(article);
        when(articleService.getPublicArticleDetail(34L, null, null))
                .thenReturn(PublicArticleDetailVO.builder().seo(metadata).build());

        var html = Jsoup.parse(service.renderHtml("/articles/34"));

        assertEquals("正文内容", html.selectFirst("meta[name=description]").attr("content"));
        assertTrue(html.select("meta[property=og:image]").isEmpty());
    }

    @Test
    void templateUpdatesShouldBeReadWithoutRestartAndMissingTemplateShouldFail() throws Exception {
        Files.writeString(template, "<html><body><div id=app></div><script src=\"/assets/new.js\"></script></body></html>");
        assertTrue(service.renderHtml("/").contains("/assets/new.js"));
        Files.delete(template);
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.renderHtml("/")).getStatusCode().value());
    }

    @Test
    void metadataShouldNotReadTemplateOrLoadHomeArticleList() {
        seoProperties.setHtmlTemplate("file:/missing/index.html");

        assertEquals("https://blog.example.com/", service.getMetadata("/").getCanonicalUrl());
        verifyNoInteractions(articleMapper, articleService);
    }

    @Test
    void publicPagesShouldOnlyInjectHeadWithoutReadingContentOrLinks() {
        for (String path : List.of("/", "/articles", "/about", "/messages")) {
            var html = Jsoup.parse(service.renderHtml(path));
            assertEquals("https://blog.example.com" + path, html.selectFirst("link[rel=canonical]").attr("href"));
            assertTrue(html.getElementById("app").html().isEmpty());
            assertTrue(html.select("main, article, nav").isEmpty());
        }
        verifyNoInteractions(articleService, articleMapper);
    }

    @Test
    void sitemapShouldBeValidXmlWithCanonicalUrlsAndActualModificationTime() throws Exception {
        when(articleMapper.selectPublicSeoArticles()).thenReturn(List.of(Article.builder()
                .id(34L).updatedAt(OffsetDateTime.parse("2026-10-08T00:00:00Z")).build()));
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var xml = factory.newDocumentBuilder().parse(new ByteArrayInputStream(service.getSitemap().getBytes(StandardCharsets.UTF_8)));

        assertEquals("http://www.sitemaps.org/schemas/sitemap/0.9", xml.getDocumentElement().getNamespaceURI());
        assertEquals(5, xml.getElementsByTagName("url").getLength());
        assertEquals("2026-10-08T00:00Z", xml.getElementsByTagName("lastmod").item(0).getTextContent());
        assertTrue(service.getSitemap().contains("https://blog.example.com/articles/34"));
        assertTrue(service.getRobots().contains("Disallow: /admin"));
        assertTrue(service.getRobots().contains("Sitemap: https://blog.example.com/sitemap.xml"));
    }

    @Test
    void robotsShouldAllowPublicRenderingApisWithoutAllowingPrivateApis() {
        String robots = service.getRobots();
        for (String path : List.of("/api/v1/articles", "/api/v1/comments/", "/api/v1/categories", "/api/v1/tags",
                "/api/v1/about", "/api/v1/messages", "/api/v1/seo", "/api/v1/users/*/public-profile")) {
            assertTrue(robots.lines().anyMatch(line -> line.equals("Allow: " + path)));
        }
        for (String path : List.of("/admin", "/auth", "/user", "/api/")) {
            assertTrue(robots.lines().anyMatch(line -> line.equals("Disallow: " + path)));
        }
        assertFalse(robots.lines().anyMatch(line -> line.equals("Allow: /api/")));
        assertFalse(robots.contains("Allow: /api/v1/admin"));
        assertFalse(robots.contains("Allow: /api/v1/auth"));
        assertFalse(robots.contains("Allow: /api/v1/users/me"));
        assertFalse(robots.contains("Allow: /api/v1/notifications"));
        assertTrue(robots.contains("Sitemap: https://blog.example.com/sitemap.xml"));
    }

    @Test
    void siteOriginShouldRejectCredentialsPathsAndQuery() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            for (String url : List.of("https://evil:password@example.com", "https://example.com/path",
                    "https://example.com?query=1", "https://example.com#fragment", "file:/tmp/file")) {
                blogProperties.setSiteUrl(URI.create(url));
                assertFalse(factory.getValidator().validate(blogProperties).isEmpty());
            }
        }
    }

    private PublicArticleDetailVO article() {
        var article = Article.builder().id(34L).title("文章 <标题>").summary("摘要 & 描述")
                .contentHtml("<p>正文内容</p>").coverUrl("/covers/34.png")
                .publishedAt(OffsetDateTime.parse("2026-10-08T00:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-10-08T01:00:00Z")).build();
        return PublicArticleDetailVO.builder().seo(new SeoMetadataFactory(blogProperties).fromArticle(article)).build();
    }
}
