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
public class DashboardMetricTrendVO {

    private List<DashboardTrendPointVO> day;

    private List<DashboardTrendPointVO> week;

    private List<DashboardTrendPointVO> month;

    private List<DashboardTrendPointVO> year;
}
