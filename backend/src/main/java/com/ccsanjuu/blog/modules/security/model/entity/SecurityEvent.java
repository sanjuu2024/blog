package com.ccsanjuu.blog.modules.security.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccsanjuu.blog.modules.security.model.enums.SecurityEventOutcome;
import com.ccsanjuu.blog.modules.security.model.enums.SecurityEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blog_security_event")
public class SecurityEvent {

    private Long id;

    private SecurityEventType eventType;

    private SecurityEventOutcome outcome;

    private Long userId;

    private Long actorId;

    private String account;

    private String ip;

    private String userAgent;

    private String requestMethod;

    private String requestPath;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
