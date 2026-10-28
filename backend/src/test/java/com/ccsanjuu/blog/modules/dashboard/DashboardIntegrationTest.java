package com.ccsanjuu.blog.modules.dashboard;

import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardVO;
import com.ccsanjuu.blog.modules.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class DashboardIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void dashboardShouldReturnAllSummaryTrendAndRankingSections() {
        DashboardVO dashboard = dashboardService.getDashboard();

        assertNotNull(dashboard.getSummary());
        assertNotNull(dashboard.getTrends());
        assertEquals(30, dashboard.getTrends().getUsers().getDay().size());
        assertNotNull(dashboard.getTrends().getUsers().getDay().getLast().getIncrement());
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

    @Test
    void userTrendCountsAccountsThatWereLaterLogicallyDeleted() {
        DashboardVO before = dashboardService.getDashboard();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        var createdAt = LocalDate.now(ZoneId.of("Asia/Shanghai"))
                .atTime(12, 0)
                .atZone(ZoneId.of("Asia/Shanghai"))
                .toOffsetDateTime();

        jdbcTemplate.update("""
                        INSERT INTO blog_user (
                            username, nickname, email, password_hash, created_at, updated_at, deleted_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                "trend_" + suffix,
                "Trend Test",
                "trend_" + suffix + "@example.test",
                "test-password-hash",
                createdAt,
                createdAt,
                createdAt
        );

        DashboardVO after = dashboardService.getDashboard();

        assertEquals(before.getSummary().getUserCount(), after.getSummary().getUserCount());
        assertEquals(
                before.getTrends().getUsers().getDay().getLast().getIncrement() + 1,
                after.getTrends().getUsers().getDay().getLast().getIncrement()
        );
    }
}
