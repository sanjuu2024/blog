package com.ccsanjuu.blog.modules.article.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blog_article")
public class Article {

    private Long id;

    private String title;

    private String slug;

    private String summary;

    private String contentMd;

    private String contentHtml;

    private String contentText;

    private String coverUrl;

    private ArticleStatus status;

    private Long categoryId;

    private Long authorId;

    private Boolean isTop;

    private Boolean allowComment;

    private Integer viewCount;

    private Integer commentCount;

    private Integer likeCount;

    private Integer favoriteCount;

    private OffsetDateTime publishedAt;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
