package com.ccsanjuu.blog.modules.auth.model.dto;

import com.ccsanjuu.blog.common.constants.ValidationConstants;
import jakarta.validation.constraints.Email;
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
public class RegisterRequestDTO {

    public RegisterRequestDTO(String username, String email, String password) {
        this(username, email, password, null);
    }

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在 2 到 20 个字符之间")
    @Pattern(regexp = ValidationConstants.USERNAME_PATTERN, message = ValidationConstants.USERNAME_MESSAGE)
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(regexp = ValidationConstants.EMAIL_PATTERN, message = ValidationConstants.EMAIL_MESSAGE)
    @Size(max = 255, message = "邮箱长度不能超过 255 个字符")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在 6 到 32 个字符之间")
    @Pattern(regexp = ValidationConstants.PASSWORD_PATTERN, message = ValidationConstants.PASSWORD_MESSAGE)
    private String password;

    @NotBlank(message = "隐私政策版本不能为空")
    @Pattern(
            regexp = ValidationConstants.PRIVACY_POLICY_VERSION_PATTERN,
            message = ValidationConstants.PRIVACY_POLICY_VERSION_MESSAGE
    )
    private String privacyPolicyVersion;
}
