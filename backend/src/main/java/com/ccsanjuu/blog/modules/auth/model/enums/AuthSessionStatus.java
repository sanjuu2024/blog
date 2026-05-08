package com.ccsanjuu.blog.modules.auth.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AuthSessionStatus {
    ACTIVE("ACTIVE", "Active session"),
    REVOKED("REVOKED", "Revoked session"),
    EXPIRED("EXPIRED", "Expired session");

    @EnumValue
    @JsonValue
    private final String value;

    private final String description;

    AuthSessionStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
