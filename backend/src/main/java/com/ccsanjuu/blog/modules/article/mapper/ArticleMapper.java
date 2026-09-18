package com.ccsanjuu.blog.modules.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.modules.article.model.bo.CategoryArticleCountBO;
import com.ccsanjuu.blog.modules.article.model.bo.PublicArticleSearchBO;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
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

    /**
     * 使用 zhparser 查询公开文章，并生成包含命中标记的标题和摘要片段。
     *
     * @param page 分页参数
     * @param keyword 已去除首尾空白的搜索关键词
     * @param categoryIds 允许查询的二级分类 ID 列表，保证非空
     * @param articleIds 包含全部筛选标签的文章 ID；未筛选标签时为空
     * @param isTop 是否只查询置顶或非置顶文章；为空时不限制
     * @param prioritizeTop 是否优先展示置顶文章
     * @param prioritizeRelevance 是否优先展示全文检索相关度
     * @return 全文检索分页结果
     */
    Page<PublicArticleSearchBO> selectPublicArticleSearchPage(
            Page<PublicArticleSearchBO> page,
            @Param("keyword") String keyword,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("articleIds") List<Long> articleIds,
            @Param("isTop") Boolean isTop,
            @Param("prioritizeTop") boolean prioritizeTop,
            @Param("prioritizeRelevance") boolean prioritizeRelevance
    );
}
