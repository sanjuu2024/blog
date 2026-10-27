package com.ccsanjuu.blog.modules.friendlink.model.dto;

import com.ccsanjuu.blog.common.api.PageQuery;
import com.ccsanjuu.blog.modules.friendlink.model.enums.FriendLinkSection;
import com.ccsanjuu.blog.modules.friendlink.model.enums.FriendLinkStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminFriendLinkQueryDTO extends PageQuery {

    @Size(max = 100, message = "友链名称关键字长度不能超过 100 个字符")
    private String keyword;

    private FriendLinkSection section;

    private FriendLinkStatus status;
}
