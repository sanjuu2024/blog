package com.ccsanjuu.blog.modules.privacy.service.impl;

import com.ccsanjuu.blog.modules.article.support.ArticleContentRenderer;
import com.ccsanjuu.blog.modules.privacy.model.vo.PrivacyPolicyVO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivacyPolicyServiceImplTest {

    private final ArticleContentRenderer articleContentRenderer = new ArticleContentRenderer();

    @Test
    void shouldLoadRenderAndVersionPrivacyPolicy() {
        PrivacyPolicyServiceImpl service = service("# 隐私政策\n\n正文");

        PrivacyPolicyVO result = service.getPrivacyPolicy();

        assertTrue(result.getVersion().matches("sha256:[0-9a-f]{64}"));
        assertTrue(result.getContentHtml().contains("<h1>隐私政策</h1>"));
        assertTrue(result.getContentText().contains("正文"));
        assertEquals(result.getVersion(), service("# 隐私政策\n\n正文").getPrivacyPolicy().getVersion());
    }

    @Test
    void shouldRejectBlankPrivacyPolicy() {
        assertThrows(IllegalStateException.class, () -> service("   \n"));
    }

    @Test
    void shouldRejectMissingPrivacyPolicy() {
        assertThrows(
                IllegalStateException.class,
                () -> new PrivacyPolicyServiceImpl(
                        articleContentRenderer,
                        new ClassPathResource("content/missing-privacy-policy.md")
                )
        );
    }

    private PrivacyPolicyServiceImpl service(String markdown) {
        return new PrivacyPolicyServiceImpl(
                articleContentRenderer,
                new ByteArrayResource(markdown.getBytes(StandardCharsets.UTF_8))
        );
    }
}
