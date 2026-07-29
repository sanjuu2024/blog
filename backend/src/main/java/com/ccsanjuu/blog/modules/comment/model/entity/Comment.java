package com.ccsanjuu.blog.modules.comment.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blog_comment")
public class Comment {

    private Long id;

    private Long articleId;

    private Long userId;

    private Long parentId;

    private Long rootId;

    private String content;

    private CommentStatus status;

    private Long reviewedBy;

    private OffsetDateTime reviewedAt;

    private String moderationReason;

    private Long deletedBy;

    private OffsetDateTime deletedAt;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
