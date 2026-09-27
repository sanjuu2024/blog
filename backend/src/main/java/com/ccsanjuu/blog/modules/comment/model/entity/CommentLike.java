package com.ccsanjuu.blog.modules.comment.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blog_comment_like")
public class CommentLike {

    private Long id;

    private Long commentId;

    private Long userId;

    private OffsetDateTime createdAt;
}
