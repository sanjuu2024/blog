package com.ccsanjuu.blog.modules.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.bo.CategoryArticleCountBO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
    /**
     * 根据所给的二级分类 id 列表，给出其中每一个二级分类对应的文章数
     *
     * @param categoryIds 保证非空
     * @param published 为 true 则文章数只统计已发表文章，为 false 则文章数统计所有状态的文章
     * @return
     */
    List<CategoryArticleCountBO> getArticleCountByCategoryIds(@Param("categoryIds") List<Long> categoryIds, @Param("published") boolean published);
}
