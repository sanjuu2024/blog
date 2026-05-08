package com.ccsanjuu.blog.modules.auth.model.dto;

import com.ccsanjuu.blog.common.constants.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "登录账号不能为空")
    @Size(max = 255, message = "登录账号长度不能超过 255 个字符")
    @Pattern(regexp = ValidationConstants.ACCOUNT_PATTERN, message = ValidationConstants.ACCOUNT_MESSAGE)
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在 6 到 20 个字符之间")
    @Pattern(regexp = ValidationConstants.PASSWORD_PATTERN, message = ValidationConstants.PASSWORD_MESSAGE)
    private String password;
}
