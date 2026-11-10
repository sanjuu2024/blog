package com.ccsanjuu.blog.modules.user.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatedUserProfileVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String avatarUrl;

    private String bio;

    private OffsetDateTime updatedAt;
}
