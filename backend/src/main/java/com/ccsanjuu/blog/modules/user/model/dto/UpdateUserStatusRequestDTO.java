package com.ccsanjuu.blog.modules.user.model.dto;

import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequestDTO {

    @NotNull(message = "用户状态不能为空")
    private UserStatus status;
}
