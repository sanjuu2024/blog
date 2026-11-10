package com.ccsanjuu.blog.modules.seo;

import com.ccsanjuu.blog.config.SecurityConfig;
import com.ccsanjuu.blog.config.WebMvcConfig;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.seo.controller.PublicHtmlController;
import com.ccsanjuu.blog.modules.seo.controller.SeoController;
import com.ccsanjuu.blog.modules.seo.model.vo.SeoMetadataVO;
import com.ccsanjuu.blog.modules.seo.service.SeoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.net.URI;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PublicHtmlController.class, SeoController.class})
@ContextConfiguration(classes = {PublicHtmlController.class, SeoController.class, WebMvcConfig.class, SecurityConfig.class})
class PublicHomeRoutingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeoService seoService;

    @MockitoBean
    private TokenVersionService tokenVersionService;

    @MockitoBean
    private SecretKey jwtSigningKey;

    @Test
    void anonymousLegacyHomeShouldRedirectToRootWithoutApiPrefixOrAuthentication() throws Exception {
        mockMvc.perform(get(URI.create("/home?from=mail&name=%E9%9D%92%E7%A6%BE")))
                .andExpect(status().isPermanentRedirect())
                .andExpect(header().string("Location", "/?from=mail&name=%E9%9D%92%E7%A6%BE"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(""));
        verifyNoInteractions(seoService, tokenVersionService);
    }

    @Test
    void anonymousRootAndMetadataApiShouldRemainPublicAtTheirCanonicalPaths() throws Exception {
        when(seoService.renderHtml("/")).thenReturn("<head><link rel=\"canonical\" href=\"https://blog.example.com/\"></head>");
        when(seoService.getMetadata("/")).thenReturn(SeoMetadataVO.builder()
                .title("首页").canonicalUrl("https://blog.example.com/").type("website").build());

        mockMvc.perform(get("/?from=mail"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"https://blog.example.com/\"")));
        mockMvc.perform(get("/api/v1/seo").param("path", "/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canonicalUrl").value("https://blog.example.com/"));
    }
}
