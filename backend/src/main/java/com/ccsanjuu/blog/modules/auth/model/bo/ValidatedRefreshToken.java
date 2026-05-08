package com.ccsanjuu.blog.modules.auth.model.bo;

import com.ccsanjuu.blog.modules.auth.model.entity.AuthSession;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidatedRefreshToken {
    String refreshToken;
    Long userId;
    String tokenJti;
    AuthSession authSession;
}
