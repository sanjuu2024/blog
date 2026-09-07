package com.ccsanjuu.blog.modules.message.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccsanjuu.blog.modules.message.model.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blog_message_board")
public class Message {

    private Long id;

    private Long userId;

    private Long parentId;

    private String nickname;

    private String email;

    private String content;

    private MessageStatus status;

    private Boolean notifyOnReply;

    private String unsubscribeToken;

    private String moderationReason;

    private Long reviewedBy;

    private OffsetDateTime reviewedAt;

    private Long deletedBy;

    private OffsetDateTime deletedAt;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
