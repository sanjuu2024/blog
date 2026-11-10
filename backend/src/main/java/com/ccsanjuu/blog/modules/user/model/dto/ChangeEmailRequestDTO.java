package com.ccsanjuu.blog.modules.user.model.dto;

import com.ccsanjuu.blog.common.constants.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改当前用户邮箱的请求参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEmailRequestDTO {

    @NotBlank(message = "当前密码不能为空")
    @Size(min = 6, max = 32, message = "当前密码长度必须在 6 到 32 个字符之间")
    @Pattern(regexp = ValidationConstants.PASSWORD_PATTERN, message = ValidationConstants.PASSWORD_MESSAGE)
    private String currentPassword;

    @NotBlank(message = "新邮箱不能为空")
    @Email(regexp = ValidationConstants.EMAIL_PATTERN, message = ValidationConstants.EMAIL_MESSAGE)
    @Size(max = 255, message = "邮箱长度不能超过 255 个字符")
    private String newEmail;

    @NotBlank(message = "邮箱验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "邮箱验证码必须是 6 位数字")
    private String verificationCode;
}
