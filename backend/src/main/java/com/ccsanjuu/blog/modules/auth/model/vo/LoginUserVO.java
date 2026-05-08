package com.ccsanjuu.blog.modules.auth.model.vo;

import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private UserRole role;

    private UserStatus status;

    private String avatarUrl;

    private String bio;
}
