package com.ccsanjuu.blog.modules.article.model.dto;

import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateArticleStatusRequestDTO {

    @NotNull(message = "文章状态不能为空")
    private ArticleStatus status;
}
