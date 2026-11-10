package com.ccsanjuu.blog.modules.dashboard.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO {

    private DashboardSummaryVO summary;

    private DashboardTrendsVO trends;

    private List<DashboardArticleRankVO> topByViews;

    private List<DashboardArticleRankVO> topByLikes;

    private List<DashboardArticleRankVO> topByComments;

    private List<DashboardBreakdownVO> articleCategoryDistribution;

    private List<DashboardBreakdownVO> articleStatusDistribution;

    private List<DashboardBreakdownVO> articleLikeActorDistribution;

    private List<DashboardBreakdownVO> commentStatusDistribution;

    private List<DashboardBreakdownVO> messageStatusDistribution;
}
