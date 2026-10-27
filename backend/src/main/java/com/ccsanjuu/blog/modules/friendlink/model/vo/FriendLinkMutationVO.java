package com.ccsanjuu.blog.modules.friendlink.model.vo;

import com.ccsanjuu.blog.modules.friendlink.model.enums.FriendLinkSection;
import com.ccsanjuu.blog.modules.friendlink.model.enums.FriendLinkStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendLinkMutationVO {

    private Long id;

    private String name;

    private String url;

    private String logoUrl;

    private String description;

    private FriendLinkSection section;

    private Integer sortNo;

    private FriendLinkStatus status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
