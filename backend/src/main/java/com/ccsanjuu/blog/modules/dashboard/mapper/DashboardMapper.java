package com.ccsanjuu.blog.modules.dashboard.mapper;

import com.ccsanjuu.blog.modules.dashboard.model.bo.DashboardArticleRankBO;
import com.ccsanjuu.blog.modules.dashboard.model.bo.DashboardBreakdownBO;
import com.ccsanjuu.blog.modules.dashboard.model.bo.DashboardTrendEventBO;
import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DashboardMapper {

    /**
     * 查询 Dashboard 当前总量。
     *
     * @return 当前总量
     */
    DashboardSummaryVO selectSummary();

    /**
     * 查询趋势窗口内的统计事件。
     *
     * @param startDate 起始日期，包含
     * @param endDateExclusive 结束日期，不包含
     * @return 按日期和指标聚合的事件数量
     */
    List<DashboardTrendEventBO> selectTrendEvents(
            @Param("startDate") LocalDate startDate,
            @Param("endDateExclusive") LocalDate endDateExclusive
    );

    /**
     * 查询趋势窗口开始前的累计基数。
     *
     * @param startDate 起始日期，不包含
     * @return 各指标在起始日期前的累计数量
     */
    List<DashboardTrendEventBO> selectTrendBaselines(@Param("startDate") LocalDate startDate);

    /**
     * 查询有效浏览数最高的已发布文章。
     *
     * @return 文章排名
     */
    List<DashboardArticleRankBO> selectTopByViews();

    /**
     * 查询文章点赞数最高的已发布文章。
     *
     * @return 文章排名
     */
    List<DashboardArticleRankBO> selectTopByLikes();

    /**
     * 查询评论数最高的已发布文章。
     *
     * @return 文章排名
     */
    List<DashboardArticleRankBO> selectTopByComments();

    /**
     * 查询已发布文章的一级分类分布。
     *
     * @return 分类分布
     */
    List<DashboardBreakdownBO> selectArticleCategoryDistribution();

    /**
     * 查询文章状态分布。
     *
     * @return 文章状态分布
     */
    List<DashboardBreakdownBO> selectArticleStatusDistribution();

    /**
     * 查询当前仍保留的文章点赞主体分布。
     *
     * @return 点赞主体分布
     */
    List<DashboardBreakdownBO> selectArticleLikeActorDistribution();

    /**
     * 查询评论审核状态分布。
     *
     * @return 评论状态分布
     */
    List<DashboardBreakdownBO> selectCommentStatusDistribution();

    /**
     * 查询留言审核状态分布。
     *
     * @return 留言状态分布
     */
    List<DashboardBreakdownBO> selectMessageStatusDistribution();
}
