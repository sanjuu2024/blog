package com.ccsanjuu.blog.modules.category.model.dto;

import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CategoryUpsertRequestDTO {

    @Positive(message = "父分类 ID 必须大于 0")
    private Long parentId;

    @NotNull(message = "分类层级不能为空")
    @Min(value = 1, message = "分类层级只能为 1 或 2")
    @Max(value = 2, message = "分类层级只能为 1 或 2")
    private Integer level;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过 50 个字符")
    private String name;

    @Size(max = 255, message = "分类描述长度不能超过 255 个字符")
    private String description;

    private Integer sortNo;

    private CategoryStatus status;
}
