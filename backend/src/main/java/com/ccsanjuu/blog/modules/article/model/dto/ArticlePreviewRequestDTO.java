package com.ccsanjuu.blog.modules.article.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未保存正文的预览请求，不依赖文章 ID 或发布表单的完整性。
 */
@Data
@NoArgsConstructor
public class ArticlePreviewRequestDTO {

    @NotNull(message = "预览正文不能为 null")
    private String contentMd;
}
