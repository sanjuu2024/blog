package com.ccsanjuu.blog.modules.dashboard;

import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardVO;
import com.ccsanjuu.blog.modules.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class DashboardIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Test
    void dashboardShouldReturnAllSummaryTrendAndRankingSections() {
        DashboardVO dashboard = dashboardService.getDashboard();

        assertNotNull(dashboard.getSummary());
        assertNotNull(dashboard.getTrends());
        assertEquals(30, dashboard.getTrends().getUsers().getDay().size());
        assertEquals(12, dashboard.getTrends().getUsers().getWeek().size());
        assertEquals(12, dashboard.getTrends().getUsers().getMonth().size());
        assertEquals(5, dashboard.getTrends().getUsers().getYear().size());
        assertNotNull(dashboard.getTopByViews());
        assertNotNull(dashboard.getTopByLikes());
        assertNotNull(dashboard.getTopByComments());
        assertNotNull(dashboard.getArticleCategoryDistribution());
        assertNotNull(dashboard.getArticleStatusDistribution());
        assertNotNull(dashboard.getArticleLikeActorDistribution());
        assertNotNull(dashboard.getCommentStatusDistribution());
        assertNotNull(dashboard.getMessageStatusDistribution());
    }
}
