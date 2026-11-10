package com.ccsanjuu.blog.modules.article.model.dto;

import com.ccsanjuu.blog.common.api.PageQuery;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminArticleQueryDTO extends PageQuery {

    @Size(max = 200, message = "文章标题关键字长度不能超过 200 个字符")
    private String title;

    @Positive(message = "分类 ID 必须大于 0")
    private Long categoryId;

    private ArticleStatus status;

    private Boolean isTop;
}
