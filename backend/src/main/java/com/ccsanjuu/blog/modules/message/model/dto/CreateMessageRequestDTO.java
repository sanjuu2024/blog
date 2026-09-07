package com.ccsanjuu.blog.modules.message.model.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMessageRequestDTO {

    private String nickname;

    private String email;

    @jakarta.validation.constraints.NotBlank(message = "留言内容不能为空")
    @Size(max = 1000, message = "留言内容长度不能超过 1000 个字符")
    private String content;

    private Boolean notifyOnReply = false;
}
