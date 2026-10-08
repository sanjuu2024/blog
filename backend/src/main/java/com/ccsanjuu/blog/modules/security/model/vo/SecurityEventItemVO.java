package com.ccsanjuu.blog.modules.security.model.vo;

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
public class SecurityEventItemVO {

    private Long id;

    private SecurityEventType eventType;

    private SecurityEventOutcome outcome;

    private Long userId;

    private String userUsername;

    private String userNickname;

    private Boolean userDeleted;

    private Long actorId;

    private String actorUsername;

    private String actorNickname;

    private Boolean actorDeleted;

    private String account;

    private String ip;

    private String userAgent;

    private String requestMethod;

    private String requestPath;

    private String browser;

    private String operatingSystem;

    private String device;

    private String description;

    private OffsetDateTime createdAt;
}
