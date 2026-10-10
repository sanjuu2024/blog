package com.ccsanjuu.blog.modules.seo.controller;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.modules.seo.model.vo.SeoMetadataVO;
import com.ccsanjuu.blog.modules.seo.service.SeoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PublicHtmlControllerTest {

    @Mock
    private SeoService seoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PublicHtmlController(seoService), new SeoController(seoService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void htmlShouldHaveUtf8AndNoStoreWithoutIncludingQueryInCanonicalPath() throws Exception {
        when(seoService.renderHtml("/articles/34")).thenReturn("<h1>中文文章</h1>");
        mockMvc.perform(get("/articles/34?replyId=51"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string("<h1>中文文章</h1>"));
    }

    @Test
    void disabledCategoryShouldReturnReal404Html() throws Exception {
        when(seoService.renderHtml("/articles/34")).thenThrow(new BizException(ResultCode.ARTICLE_CATEGORY_DISABLED));
        mockMvc.perform(get("/articles/34")).andExpect(status().isNotFound())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("noindex")));
    }

    @Test
    void templateUnavailableShouldNotLeakInternalDetails() throws Exception {
        when(seoService.renderHtml("/")).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "/private/path"));
        mockMvc.perform(get("/")).andExpect(status().isServiceUnavailable())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/private/path"))));
    }

    @Test
    void metadataShouldUseExistingJsonEnvelopeAndNoStore() throws Exception {
        when(seoService.getMetadata("/articles/34")).thenReturn(SeoMetadataVO.builder()
                .title("文章标题").canonicalUrl("https://blog.example.com/articles/34").type("article").build());
        mockMvc.perform(get("/seo").param("path", "/articles/34"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("文章标题"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void sitemapAndRobotsShouldUseCorrectContentTypes() throws Exception {
        when(seoService.getSitemap()).thenReturn("<urlset />");
        when(seoService.getRobots()).thenReturn("User-agent: *\n");
        mockMvc.perform(get("/sitemap.xml")).andExpect(status().isOk())
                .andExpect(content().contentType("application/xml;charset=UTF-8"));
        mockMvc.perform(get("/robots.txt")).andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"));
    }
}
