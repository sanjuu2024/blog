package com.ccsanjuu.blog.modules.about.model.dto;

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
public class UpdateAboutPageRequestDTO {

    @NotBlank(message = "关于页内容不能为空")
    @Size(max = 100000, message = "关于页内容不能超过 100000 个字符")
    private String contentMd;
}
