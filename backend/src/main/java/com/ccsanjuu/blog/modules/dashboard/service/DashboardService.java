package com.ccsanjuu.blog.modules.dashboard.service;

import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardVO;

public interface DashboardService {

    /**
     * 获取管理员 Dashboard 数据。
     *
     * @return Dashboard 总量、趋势和文章排名
     */
    DashboardVO getDashboard();
}
