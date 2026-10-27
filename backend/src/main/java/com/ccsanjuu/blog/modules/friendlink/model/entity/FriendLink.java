package com.ccsanjuu.blog.modules.friendlink.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("blog_friend_link")
public class FriendLink {

    private Long id;

    private String name;

    private String url;

    private String logoUrl;

    private String description;

    private FriendLinkSection section;

    private Integer sortNo;

    private FriendLinkStatus status;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
