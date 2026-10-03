package com.ccsanjuu.blog.modules.privacy.service.impl;

import com.ccsanjuu.blog.modules.article.support.ArticleContentRenderer;
import com.ccsanjuu.blog.modules.privacy.mapper.PrivacyPolicyVersionMapper;
import com.ccsanjuu.blog.modules.privacy.model.entity.PrivacyPolicyVersion;
import com.ccsanjuu.blog.modules.privacy.model.vo.PrivacyPolicyVO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrivacyPolicyServiceImplTest {

    private final ArticleContentRenderer articleContentRenderer = new ArticleContentRenderer();
    private final PrivacyPolicyVersionMapper privacyPolicyVersionMapper = mock(PrivacyPolicyVersionMapper.class);

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
                        privacyPolicyVersionMapper,
                        new ClassPathResource("content/missing-privacy-policy.md")
                )
        );
    }

    @Test
    void shouldCompareCurrentPrivacyPolicyVersion() {
        PrivacyPolicyServiceImpl service = service("# 隐私政策\n\n正文");
        String version = service.getPrivacyPolicy().getVersion();

        assertTrue(service.compareTo(version));
        assertTrue(!service.compareTo("sha256:" + "0".repeat(64)));
        assertTrue(!service.compareTo(null));
    }

    @Test
    void shouldArchiveCurrentPrivacyPolicyVersion() {
        PrivacyPolicyServiceImpl service = service("# 隐私政策\n\n正文");

        verify(privacyPolicyVersionMapper).insert(any(PrivacyPolicyVersion.class));
        assertTrue(service.getPrivacyPolicy().getVersion().startsWith("sha256:"));
    }

    private PrivacyPolicyServiceImpl service(String markdown) {
        when(privacyPolicyVersionMapper.selectById(any())).thenReturn(null);
        return new PrivacyPolicyServiceImpl(
                articleContentRenderer,
                privacyPolicyVersionMapper,
                new ByteArrayResource(markdown.getBytes(StandardCharsets.UTF_8))
        );
    }
}
