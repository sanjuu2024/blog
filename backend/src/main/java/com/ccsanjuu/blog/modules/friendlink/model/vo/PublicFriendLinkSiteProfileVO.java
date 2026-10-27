package com.ccsanjuu.blog.modules.friendlink.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 友链申请页展示的本站资料。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicFriendLinkSiteProfileVO {

    private String name;

    private String url;

    private String description;

    private String logoUrl;
}
