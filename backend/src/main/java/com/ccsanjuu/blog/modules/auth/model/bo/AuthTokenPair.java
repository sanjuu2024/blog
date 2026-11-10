package com.ccsanjuu.blog.modules.auth.model.bo;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class AuthTokenPair{
    String accessToken;
    OffsetDateTime accessTokenExpiresAt;
    String refreshToken;
    OffsetDateTime refreshTokenExpiresAt;
    String refreshTokenJti;
}
