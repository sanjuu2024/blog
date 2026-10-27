package com.ccsanjuu.blog.modules.friendlink.model.vo;

import com.ccsanjuu.blog.modules.friendlink.model.enums.FriendLinkSection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicFriendLinkSectionVO {

    private FriendLinkSection section;

    private String name;

    private List<PublicFriendLinkItemVO> records;
}
