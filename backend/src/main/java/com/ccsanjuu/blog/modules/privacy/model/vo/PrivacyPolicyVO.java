package com.ccsanjuu.blog.modules.privacy.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyPolicyVO {

    private String version;

    private String contentHtml;

    private String contentText;
}
