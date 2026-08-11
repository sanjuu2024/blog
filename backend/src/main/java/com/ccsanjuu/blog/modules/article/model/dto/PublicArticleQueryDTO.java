package com.ccsanjuu.blog.modules.article.model.dto;

import com.ccsanjuu.blog.modules.article.model.enums.PublicArticleSort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class PublicArticleQueryDTO {

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于等于 1")
    private Integer pageNum = 1;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数必须大于等于 1")
    @Max(value = 20, message = "每页条数不能超过 20")
    private Integer pageSize = 10;

    @Size(max = 100, message = "搜索关键词长度不能超过 100 个字符")
    private String keyword;

    @Positive(message = "分类 ID 必须大于 0")
    private Long categoryId;

    private List<@Positive(message = "标签 ID 必须大于 0") Long> tagIds;

    private Boolean isTop;

    private PublicArticleSort sort = PublicArticleSort.DEFAULT;
}
