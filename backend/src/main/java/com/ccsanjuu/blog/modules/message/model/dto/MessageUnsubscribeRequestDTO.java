package com.ccsanjuu.blog.modules.message.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageUnsubscribeRequestDTO {

    @NotBlank(message = "退订令牌不能为空")
    @Size(min = 20, max = 128, message = "退订令牌长度必须在 20-128 个字符之间")
    private String token;
}
