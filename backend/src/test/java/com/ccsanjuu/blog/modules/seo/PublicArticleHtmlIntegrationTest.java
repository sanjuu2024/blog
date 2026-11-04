package com.ccsanjuu.blog.modules.seo;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.config.SecurityConfig;
import com.ccsanjuu.blog.config.WebMvcConfig;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.seo.controller.PublicHtmlController;
import com.ccsanjuu.blog.modules.seo.controller.SeoController;
import com.ccsanjuu.blog.modules.seo.service.impl.SeoServiceImpl;
import com.ccsanjuu.blog.properties.BlogProperties;
import com.ccsanjuu.blog.properties.SeoProperties;
import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PublicHtmlController.class, SeoController.class}, properties = {
        "blog.app-name=测试站点", "blog.site-url=https://blog.example.com", "blog.description=测试描述",
        "blog.logo-url=https://blog.example.com/logo.png", "blog.seo.html-template=classpath:seo/index.html"
})
@ContextConfiguration(classes = {PublicHtmlController.class, SeoController.class, SeoServiceImpl.class,
        WebMvcConfig.class, SecurityConfig.class, GlobalExceptionHandler.class, PublicArticleHtmlIntegrationTest.SiteConfig.class})
class PublicArticleHtmlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

    @MockitoBean
    private ArticleMapper articleMapper;

    @MockitoBean
    private TokenVersionService tokenVersionService;

    @MockitoBean
    private SecretKey jwtSigningKey;

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties({BlogProperties.class, SeoProperties.class})
    static class SiteConfig {
    }

    @ParameterizedTest
    @EnumSource(value = ResultCode.class, names = {"ARTICLE_NOT_FOUND", "ARTICLE_NOT_VISIBLE",
            "ARTICLE_CATEGORY_NOT_FOUND", "ARTICLE_CATEGORY_DISABLED"})
    void unavailableArticleShouldReturn404VueEntryWhileMetadataApiKeepsJsonErrors(ResultCode reason) throws Exception {
        when(articleService.getPublicArticleDetail(34L, null, null)).thenThrow(new BizException(reason));

        var response = mockMvc.perform(get("/articles/34?replyId=51"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andReturn().getResponse();
        var html = Jsoup.parse(response.getContentAsString());
        assertEquals("文章不存在 - 测试站点", html.title());
        assertEquals("noindex, follow", html.selectFirst("meta[name=robots][data-seo]").attr("content"));
        assertEquals("/assets/test.js", html.selectFirst("script[type=module]").attr("src"));
        assertTrue(html.getElementById("app").html().isEmpty());
        assertTrue(html.select("h1, main, article, link[rel=canonical], meta[name=description], meta[property^=og:], meta[property^=article:]").isEmpty());
        verify(articleService).getPublicArticleDetail(34L, null, null);
        verifyNoInteractions(articleMapper, tokenVersionService);

        mockMvc.perform(get("/api/v1/seo").param("path", "/articles/34"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value(ResultCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "034", "not-an-id", "99999999999999999999999"})
    void invalidArticleIdsShouldReturn404VueEntryWithoutDatabaseLookup(String id) throws Exception {
        mockMvc.perform(get("/articles/" + id))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/assets/test.js")));
        verifyNoInteractions(articleService, articleMapper);
    }
}
