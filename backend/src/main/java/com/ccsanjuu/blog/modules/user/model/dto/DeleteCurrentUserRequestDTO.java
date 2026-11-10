package com.ccsanjuu.blog.modules.user.model.dto;

import com.ccsanjuu.blog.common.constants.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注销当前账号的请求参数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteCurrentUserRequestDTO {

    @NotBlank(message = "当前密码不能为空")
    @Size(min = 6, max = 32, message = "当前密码长度必须在 6 到 32 个字符之间")
    @Pattern(regexp = ValidationConstants.PASSWORD_PATTERN, message = ValidationConstants.PASSWORD_MESSAGE)
    private String password;
}
