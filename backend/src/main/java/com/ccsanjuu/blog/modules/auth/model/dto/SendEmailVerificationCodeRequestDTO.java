package com.ccsanjuu.blog.modules.auth.model.dto;

import com.ccsanjuu.blog.common.constants.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailVerificationCodeRequestDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(regexp = ValidationConstants.EMAIL_PATTERN, message = ValidationConstants.EMAIL_MESSAGE)
    @Size(max = 255, message = "邮箱长度不能超过 255 个字符")
    private String email;
}
