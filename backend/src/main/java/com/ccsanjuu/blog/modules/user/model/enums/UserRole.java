package com.ccsanjuu.blog.modules.user.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN("ADMIN", "管理员"),
    USER("USER", "用户");

    @EnumValue
    @JsonValue
    private final String value;

    private final String description;

    UserRole(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
