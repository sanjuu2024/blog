package com.ccsanjuu.blog.modules.dashboard.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardVO;
import com.ccsanjuu.blog.modules.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@Tag(name = "管理员 Dashboard 接口")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取管理员 Dashboard 总量、趋势和文章排名。
     *
     * @return Dashboard 数据
     */
    @GetMapping
    @Operation(description = "获取管理员 Dashboard 总量、趋势和文章排名")
    public Result<DashboardVO> getDashboard() {
        return Result.success(dashboardService.getDashboard());
    }
}
