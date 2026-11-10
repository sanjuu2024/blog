package com.ccsanjuu.blog.modules.user.model.dto;

import com.ccsanjuu.blog.common.constants.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送修改邮箱验证码的请求参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailChangeCodeRequestDTO {

    @NotBlank(message = "新邮箱不能为空")
    @Email(regexp = ValidationConstants.EMAIL_PATTERN, message = ValidationConstants.EMAIL_MESSAGE)
    @Size(max = 255, message = "邮箱长度不能超过 255 个字符")
    private String email;
}
