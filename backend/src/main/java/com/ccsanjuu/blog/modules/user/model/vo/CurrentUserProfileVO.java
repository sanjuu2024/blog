package com.ccsanjuu.blog.modules.user.model.vo;

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
public class CurrentUserProfileVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private UserRole role;

    private UserStatus status;

    private String avatarUrl;

    private String bio;

    private OffsetDateTime lastLoginAt;

    private OffsetDateTime createdAt;
}
