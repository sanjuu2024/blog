package com.ccsanjuu.blog.modules.about.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicAboutPageVO {

    private Boolean exists;

    private String contentHtml;

    private String contentText;

    private OffsetDateTime updatedAt;
}
