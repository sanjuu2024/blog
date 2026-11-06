package com.ccsanjuu.blog.modules.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.modules.article.model.bo.CategoryArticleCountBO;
import com.ccsanjuu.blog.modules.article.model.bo.PublicArticleSearchBO;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.vo.ArchiveArticleVO;
import com.ccsanjuu.blog.modules.article.model.vo.ArchiveYearVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 一次聚合浏览器当地时区的公开文章年份。
     *
     * @param timeZone 已校验的浏览器 IANA 命名时区
     * @return 年份倒序汇总
     */
    List<ArchiveYearVO> selectArchiveYears(@Param("timeZone") String timeZone);

    /**
     * 联表分页获取公开文章和分类路径。
     *
     * @param page 分页参数
     * @param start 年份起点，未按年筛选时为空
     * @param end 下一年起点，未按年筛选时为空
     * @param categoryId 一级或二级分类 ID
     * @param tagId 标签 ID
     * @return 归档分页
     */
    Page<ArchiveArticleVO> selectArchivePage(
            Page<ArchiveArticleVO> page,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("categoryId") Long categoryId,
            @Param("tagId") Long tagId
    );

    /**
     * 查询符合公开可见规则的文章供站点地图和 RSS 使用，不改变浏览计数。
     *
     * @param limit 最大记录数；为空时返回全部符合条件的文章
     * @return 符合公开文章、分类及作者可见规则的文章
     */
    List<Article> selectPublicSyndicationArticles(@Param("limit") Integer limit);

    /**
     * 仅对仍处于已发布状态的文章原子递增浏览总数。
     *
     * @param articleId 文章 ID
     * @return 更新行数
     */
    int incrementViewCount(@Param("articleId") Long articleId);

    /**
     * 原子递增已发布文章的点赞数。
     *
     * @param articleId 文章 ID
     * @return 更新行数
     */
    int incrementLikeCount(@Param("articleId") Long articleId);

    /**
     * 原子递减文章点赞数，并防止冗余计数变为负数。
     *
     * @param articleId 文章 ID
     * @return 更新行数
     */
    int decrementLikeCount(@Param("articleId") Long articleId);

    /**
     * 根据所给的二级分类 id 列表，给出其中每一个二级分类对应的文章数
     *
     * @param categoryIds 保证非空
     * @param published 为 true 则文章数只统计已发表文章，为 false 则文章数统计所有状态的文章
     * @return
     */
    List<CategoryArticleCountBO> getArticleCountByCategoryIds(@Param("categoryIds") List<Long> categoryIds, @Param("published") boolean published);

    /**
     * 合并 zhparser 全文检索与字面量包含匹配，返回公开文章及用于安全高亮的标题和摘要片段。
     *
     * @param page 分页参数
     * @param keyword 已去除首尾空白的搜索关键词
     * @param categoryIds 允许查询的二级分类 ID 列表，保证非空
     * @param articleIds 包含全部筛选标签的文章 ID；未筛选标签时为空
     * @param isTop 是否只查询置顶或非置顶文章；为空时不限制
     * @param prioritizeTop 是否优先展示置顶文章
     * @param prioritizeRelevance 是否优先展示标题命中，再按全文检索相关度排序
     * @return 搜索分页结果
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
