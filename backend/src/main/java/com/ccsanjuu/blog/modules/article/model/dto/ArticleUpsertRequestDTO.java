package com.ccsanjuu.blog.modules.article.model.dto;

import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleUpsertRequestDTO {

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题长度不能超过 200 个字符")
    private String title;

    @Size(max = 500, message = "文章摘要长度不能超过 500 个字符")
    private String summary;

    @NotBlank(message = "文章正文不能为空")
    private String contentMd;

    @NotNull(message = "文章分类不能为空")
    @Positive(message = "分类 ID 必须大于 0")
    private Long categoryId;

    private List<@Positive(message = "标签 ID 必须大于 0") Long> tagIds;

    @Size(max = 500, message = "文章封面地址长度不能超过 500 个字符")
    private String coverUrl;

    private Boolean isTop;

    @NotNull(message = "文章状态不能为空")
    private ArticleStatus status;

    private Boolean allowComment;
}
