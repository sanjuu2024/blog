package com.ccsanjuu.blog.modules.user.model.dto;

import com.ccsanjuu.blog.common.api.PageQuery;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserManagementPageQueryDTO extends PageQuery {

    @Size(max = 20, message = "用户名长度不能超过 20 个字符")
    private String username;

    @Size(max = 255, message = "邮箱长度不能超过 255 个字符")
    private String email;

    private UserRole role;

    private UserStatus status;
}
