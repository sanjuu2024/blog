package com.ccsanjuu.blog.modules.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员注销用户的请求参数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDeleteUserRequestDTO {

    @NotBlank(message = "注销原因不能为空")
    @Size(max = 255, message = "注销原因长度不能超过 255 个字符")
    private String reason;
}
