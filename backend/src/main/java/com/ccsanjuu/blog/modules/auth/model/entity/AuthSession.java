package com.ccsanjuu.blog.modules.auth.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccsanjuu.blog.modules.auth.model.enums.AuthSessionStatus;
import com.ccsanjuu.blog.modules.auth.model.enums.AuthSessionTokenType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blog_auth_session")
public class AuthSession {

    private Long id;

    private Long userId;

    private String tokenJti;

    private String tokenHash;

    private AuthSessionTokenType tokenType;

    private AuthSessionStatus status;

    private OffsetDateTime expiresAt;

    private OffsetDateTime revokedAt;

    private String ip;

    private String userAgent;

    private String deviceInfo;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
