package com.ccsanjuu.blog.modules.comment.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CommentReplyQueryDTO {

    @NotNull(message = "回复条数不能为空")
    @Min(value = 1, message = "回复条数必须大于等于 1")
    @Max(value = 20, message = "回复条数不能超过 20")
    private Integer limit = 5;

    private String cursor;
}
