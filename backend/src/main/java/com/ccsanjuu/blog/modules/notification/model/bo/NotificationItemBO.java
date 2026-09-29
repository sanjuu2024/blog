package com.ccsanjuu.blog.modules.notification.model.bo;

import com.ccsanjuu.blog.modules.notification.model.enums.NotificationType;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class NotificationItemBO {

    private Long id;

    private NotificationType type;

    private String title;

    private String content;

    private Long sourceId;

    private String authorName;

    private String originalContent;

    private Long articleId;

    private Long parentId;

    /** 仅评论回复通知返回。 */
    private Integer likeCount;

    /** 仅评论回复通知返回。 */
    private Boolean liked;

    /** 仅评论回复通知返回。 */
    private Boolean canInteract;

    private Boolean read;

    private OffsetDateTime createdAt;
}
