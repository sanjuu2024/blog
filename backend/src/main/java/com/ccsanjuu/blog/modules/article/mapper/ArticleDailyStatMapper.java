package com.ccsanjuu.blog.modules.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.article.model.entity.ArticleDailyStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface ArticleDailyStatMapper extends BaseMapper<ArticleDailyStat> {

    /**
     * 插入或原子递增文章在指定自然日的有效浏览数。
     *
     * @param articleId 文章 ID
     * @param statDate 统计日期
     * @return 更新行数
     */
    int incrementViewCount(@Param("articleId") Long articleId, @Param("statDate") LocalDate statDate);
}
