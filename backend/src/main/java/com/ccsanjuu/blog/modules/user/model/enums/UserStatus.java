package com.ccsanjuu.blog.modules.user.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserStatus {
    ACTIVE("ACTIVE", "启用"),
    DISABLED("DISABLED", "禁用");

    @EnumValue
    @JsonValue
    private final String value;

    private final String description;

    UserStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
