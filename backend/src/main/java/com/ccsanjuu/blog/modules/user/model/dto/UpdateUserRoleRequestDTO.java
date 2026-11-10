package com.ccsanjuu.blog.modules.user.model.dto;

import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleRequestDTO {

    @NotNull(message = "用户角色不能为空")
    private UserRole role;
}
