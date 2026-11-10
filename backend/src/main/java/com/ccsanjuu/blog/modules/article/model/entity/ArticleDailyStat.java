package com.ccsanjuu.blog.modules.article.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blog_article_daily_stat")
public class ArticleDailyStat {

    private Long articleId;

    private LocalDate statDate;

    private Long viewCount;
}
