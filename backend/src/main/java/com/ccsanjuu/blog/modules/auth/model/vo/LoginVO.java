package com.ccsanjuu.blog.modules.auth.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    private String accessToken;

    private OffsetDateTime accessTokenExpiresAt;

    @JsonIgnore
    private String refreshToken;

    private OffsetDateTime refreshTokenExpiresAt;

    private String tokenType;

    private LoginUserVO user;
}
