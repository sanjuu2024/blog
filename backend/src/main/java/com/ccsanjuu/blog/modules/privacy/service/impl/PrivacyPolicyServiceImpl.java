package com.ccsanjuu.blog.modules.privacy.service.impl;

import com.ccsanjuu.blog.modules.article.support.ArticleContentRenderer;
import com.ccsanjuu.blog.modules.privacy.model.vo.PrivacyPolicyVO;
import com.ccsanjuu.blog.modules.privacy.service.PrivacyPolicyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class PrivacyPolicyServiceImpl implements PrivacyPolicyService {

    private final PrivacyPolicyVO privacyPolicy;

    public PrivacyPolicyServiceImpl(
            ArticleContentRenderer articleContentRenderer,
            @Value("${blog.privacy-policy.resource:classpath:content/privacy-policy.md}") Resource resource
    ) {
        String markdown = readPrivacyPolicy(resource);
        this.privacyPolicy = PrivacyPolicyVO.builder()
                .version(sha256(markdown))
                .contentHtml(articleContentRenderer.convertMarkdownToHtml(markdown))
                .contentText(articleContentRenderer.convertToText(markdown))
                .build();
    }

    @Override
    public PrivacyPolicyVO getPrivacyPolicy() {
        return privacyPolicy;
    }

    private String readPrivacyPolicy(Resource resource) {
        try {
            String markdown = resource.getContentAsString(StandardCharsets.UTF_8).strip();
            if (!StringUtils.hasText(markdown)) {
                throw new IllegalStateException("隐私政策资源不能为空");
            }
            return markdown;
        } catch (IOException exception) {
            throw new IllegalStateException("读取隐私政策资源失败", exception);
        }
    }

    private String sha256(String content) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
