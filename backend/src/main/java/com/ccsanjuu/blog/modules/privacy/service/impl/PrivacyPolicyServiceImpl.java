package com.ccsanjuu.blog.modules.privacy.service.impl;

import com.ccsanjuu.blog.modules.article.support.ArticleContentRenderer;
import com.ccsanjuu.blog.modules.privacy.mapper.PrivacyPolicyVersionMapper;
import com.ccsanjuu.blog.modules.privacy.model.entity.PrivacyPolicyVersion;
import com.ccsanjuu.blog.modules.privacy.model.vo.PrivacyPolicyVO;
import com.ccsanjuu.blog.modules.privacy.service.PrivacyPolicyService;
import org.springframework.dao.DuplicateKeyException;
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

    /**
     * 注入 privacy-policy.md 为 privacyPolicy 对象
     * - 启动时计算规范化 Markdown 的 SHA-256，如果数据库不存在该版本，就把版本和当时的完整 Markdown 插入版本表。
     *
     * @param articleContentRenderer
     * @param privacyPolicyVersionMapper 隐私政策版本 Mapper
     * @param resource
     */
    public PrivacyPolicyServiceImpl(
            ArticleContentRenderer articleContentRenderer,
            PrivacyPolicyVersionMapper privacyPolicyVersionMapper,
            @Value("${blog.privacy-policy.resource:classpath:content/privacy-policy.md}") Resource resource
    ) {
        String markdown = readPrivacyPolicy(resource);
        String version = sha256(markdown);
        this.privacyPolicy = PrivacyPolicyVO.builder()
                .version(version)
                .contentHtml(articleContentRenderer.convertMarkdownToHtml(markdown))
                .contentText(articleContentRenderer.convertToText(markdown))
                .build();
        archivePrivacyPolicy(privacyPolicyVersionMapper, version, markdown);
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

    /**
     * 保存当前隐私政策的不可变快照，多实例并发启动时允许另一实例先完成插入。
     *
     * @param mapper 隐私政策版本 Mapper
     * @param version 内容哈希版本
     * @param markdown 规范化后的 Markdown 正文
     */
    private void archivePrivacyPolicy(
            PrivacyPolicyVersionMapper mapper,
            String version,
            String markdown
    ) {
        PrivacyPolicyVersion existing = mapper.selectById(version);
        if (existing == null) {
            try {
                mapper.insert(PrivacyPolicyVersion.builder()
                        .version(version)
                        .contentMd(markdown)
                        .build());
                return;
            } catch (DuplicateKeyException ignored) {
                existing = mapper.selectById(version);
            }
        }
        if (existing == null || !markdown.equals(existing.getContentMd())) {
            throw new IllegalStateException("隐私政策版本快照不一致");
        }
    }

    /**
     * 根据 sha256 值判断版本是否一致
     *
     * @param sha256Txt
     * @return
     */
    public boolean compareTo(String sha256Txt) {
        return sha256Txt != null && sha256Txt.equals(getPrivacyPolicy().getVersion());
    }
}
