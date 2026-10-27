package com.ccsanjuu.blog.modules.friendlink.model.vo;

import com.ccsanjuu.blog.modules.friendlink.model.enums.FriendLinkSection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicFriendLinkItemVO {

    private Long id;

    private String name;

    private String url;

    private String logoUrl;

    private String description;

    private FriendLinkSection section;

    private Integer sortNo;
}
