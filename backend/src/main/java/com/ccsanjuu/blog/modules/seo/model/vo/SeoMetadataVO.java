package com.ccsanjuu.blog.modules.seo.model.vo;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

/** 公开 HTML 与 Vue 导航共用的页面元信息。 */
@Data
@Builder
public class SeoMetadataVO {

    private String title;

    private String description;

    private String canonicalUrl;

    private String imageUrl;

    private String type;

    private OffsetDateTime publishedAt;

    private OffsetDateTime updatedAt;
}
