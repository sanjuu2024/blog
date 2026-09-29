package com.ccsanjuu.blog.modules.notification.model.vo;

import com.ccsanjuu.blog.modules.notification.model.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItemVO {

    private Long id;

    private NotificationType type;

    private String title;

    private String content;

    private Long sourceId;

    private String authorName;

    private String originalContent;

    private Long articleId;

    private Long parentId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer likeCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean liked;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean canInteract;

    private Boolean read;

    private OffsetDateTime createdAt;
}
