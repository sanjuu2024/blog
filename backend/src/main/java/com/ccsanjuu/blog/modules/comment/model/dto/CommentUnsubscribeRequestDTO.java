package com.ccsanjuu.blog.modules.comment.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentUnsubscribeRequestDTO {

    @NotBlank(message = "评论退订令牌不能为空")
    @Size(min = 20, max = 128, message = "评论退订令牌长度不合法")
    private String token;
}
