package com.ccsanjuu.blog.modules.comment.model.dto;

import com.ccsanjuu.blog.modules.comment.model.validation.CommentContentLength;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequestDTO {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2260, message = "评论原始内容长度不能超过 2260 个字符")
    @CommentContentLength
    private String content;

    @Positive(message = "父评论 ID 必须大于 0")
    private Long parentId;

    @Builder.Default
    private Boolean notifyOnReply = false;
}
