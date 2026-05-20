package com.ccsanjuu.blog.modules.category.model.dto;

import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCategoryQueryDTO {

    @Size(max = 50, message = "分类名称关键字长度不能超过 50 个字符")
    private String keyword;

    private CategoryStatus status;

    @Min(value = 1, message = "分类层级只能为 1 或 2")
    @Max(value = 2, message = "分类层级只能为 1 或 2")
    private Integer level;

    @Positive(message = "父分类 ID 必须大于 0")
    private Long parentId;
}
