package com.ccsanjuu.blog.modules.message.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMessageReplyRequestDTO {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 1000, message = "回复内容长度不能超过 1000 个字符")
    private String content;
}
