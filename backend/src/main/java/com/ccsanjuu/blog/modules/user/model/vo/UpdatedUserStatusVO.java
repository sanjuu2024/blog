package com.ccsanjuu.blog.modules.user.model.vo;

import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatedUserStatusVO {

    private Long id;

    private UserStatus status;
}
