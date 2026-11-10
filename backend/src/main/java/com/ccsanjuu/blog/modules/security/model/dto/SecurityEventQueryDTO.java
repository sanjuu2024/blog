package com.ccsanjuu.blog.modules.security.model.dto;

import com.ccsanjuu.blog.common.api.PageQuery;
import com.ccsanjuu.blog.modules.security.model.enums.SecurityEventOutcome;
import com.ccsanjuu.blog.modules.security.model.enums.SecurityEventType;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SecurityEventQueryDTO extends PageQuery {

    private SecurityEventType eventType;

    private SecurityEventOutcome outcome;

    @Positive(message = "用户 ID 必须大于 0")
    private Long userId;

    private OffsetDateTime createdAtFrom;

    private OffsetDateTime createdAtTo;
}
