package com.ccsanjuu.blog.modules.dashboard.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTrendsVO {

    private DashboardMetricTrendVO users;

    private DashboardMetricTrendVO publishedArticles;

    private DashboardMetricTrendVO views;

    private DashboardMetricTrendVO likes;

    private DashboardMetricTrendVO comments;

    private DashboardMetricTrendVO messages;
}
