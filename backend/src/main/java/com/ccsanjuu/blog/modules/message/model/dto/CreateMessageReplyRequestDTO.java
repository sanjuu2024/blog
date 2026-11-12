package com.ccsanjuu.blog.modules.message.model.dto;

import com.ccsanjuu.blog.common.validation.StickerContentLength;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMessageReplyRequestDTO {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 2260, message = "回复原始内容长度不能超过 2260 个字符")
    @StickerContentLength(message = "回复内容长度不能超过 1000 个字符，且表情不能超过 20 个")
    private String content;
}
