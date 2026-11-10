package com.ccsanjuu.blog.modules.article.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blog_article_tag")
public class ArticleTag {

    private Long articleId;

    private Long tagId;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
