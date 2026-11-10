package com.ccsanjuu.blog.modules.tag.model.dto;

import com.ccsanjuu.blog.modules.tag.model.enums.TagStatus;
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
public class TagUpsertRequestDTO {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称长度不能超过 50 个字符")
    private String name;

    @Size(max = 255, message = "标签描述长度不能超过 255 个字符")
    private String description;

    private TagStatus status;
}
