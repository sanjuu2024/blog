package com.ccsanjuu.blog.modules.user.model.vo;

import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatedUserRoleVO {

    private Long id;

    private UserRole role;
}
