package com.ccsanjuu.blog.modules.auth.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AuthSessionTokenType {
    REFRESH("REFRESH", "Refresh Token");

    @EnumValue
    @JsonValue
    private final String value;

    private final String description;

    AuthSessionTokenType(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
