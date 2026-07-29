package com.ccsanjuu.blog.modules.comment.model.dto;

import com.ccsanjuu.blog.modules.comment.model.enums.CommentModerationAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentModerationRequestDTO {

    @NotNull(message = "评论处理动作不能为空")
    private CommentModerationAction action;

    @Size(max = 255, message = "处理原因长度不能超过 255 个字符")
    private String reason;
}
