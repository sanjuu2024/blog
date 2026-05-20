package com.ccsanjuu.blog.modules.tag.model.dto;

import com.ccsanjuu.blog.modules.tag.model.enums.TagStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTagQueryDTO {

    @Size(max = 50, message = "标签名称关键字长度不能超过 50 个字符")
    private String keyword;

    private TagStatus status;
}
