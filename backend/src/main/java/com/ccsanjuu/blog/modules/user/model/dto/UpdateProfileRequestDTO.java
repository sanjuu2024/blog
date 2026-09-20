package com.ccsanjuu.blog.modules.user.model.dto;

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
public class UpdateProfileRequestDTO {

    @Size(min = 1, max = 20, message = "昵称长度必须在 1 到 20 个字符之间")
    @Pattern(regexp = ".*\\S.*", message = "昵称不能全为空白字符")
    private String nickname;

    @Size(max = 100, message = "个人简介长度不能超过 100 个字符")
    private String bio;
}
