package com.ccsanjuu.blog.modules.user.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blog_user")
public class User {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String passwordHash;

    private UserRole role;

    private UserStatus status;

    private String avatarUrl;

    private String bio;

    private Boolean emailVerified;

    private OffsetDateTime emailVerifiedAt;

    private OffsetDateTime lastLoginAt;

    private OffsetDateTime deletedAt;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
