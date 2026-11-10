package com.ccsanjuu.blog.modules.article.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ArchiveQueryDTO extends ArchiveTimeZoneQueryDTO {

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于等于 1")
    private Integer pageNum = 1;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数必须大于等于 1")
    @Max(value = 20, message = "每页条数不能超过 20")
    private Integer pageSize = 10;

    @Min(value = 1, message = "年份必须大于等于 1")
    @Max(value = 9998, message = "年份不能超过 9998")
    private Integer year;

    @Positive(message = "分类 ID 必须大于 0")
    private Long categoryId;

    @Positive(message = "标签 ID 必须大于 0")
    private Long tagId;

    /**
     * 归档各浏览模式只允许一个筛选维度。
     *
     * @return 是否最多指定一个归档条件
     */
    @AssertTrue(message = "年份、分类和标签最多选择一个")
    public boolean isSingleFilter() {
        return (year == null ? 0 : 1) + (categoryId == null ? 0 : 1) + (tagId == null ? 0 : 1) <= 1;
    }
}
