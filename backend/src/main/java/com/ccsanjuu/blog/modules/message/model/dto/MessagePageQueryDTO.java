package com.ccsanjuu.blog.modules.message.model.dto;

import com.ccsanjuu.blog.common.api.PageQuery;
import com.ccsanjuu.blog.modules.message.model.enums.MessageStatus;
import com.ccsanjuu.blog.modules.message.model.enums.MessageType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class MessagePageQueryDTO extends PageQuery {

    @Positive(message = "留言 ID 必须大于 0")
    private Long messageId;

    @Positive(message = "用户 ID 必须大于 0")
    private Long userId;

    @Size(max = 20, message = "游客昵称筛选长度不能超过 20 个字符")
    private String guestNickname;

    @Size(max = 255, message = "游客邮箱筛选长度不能超过 255 个字符")
    private String guestEmail;

    @Size(max = 1000, message = "留言内容筛选长度不能超过 1000 个字符")
    private String content;

    private MessageStatus status;

    private MessageType type;

    private OffsetDateTime createdAtFrom;

    private OffsetDateTime createdAtTo;
}
