package com.ccsanjuu.blog.modules.dashboard.service.impl;

import com.ccsanjuu.blog.modules.dashboard.mapper.DashboardMapper;
import com.ccsanjuu.blog.modules.dashboard.model.bo.DashboardArticleRankBO;
import com.ccsanjuu.blog.modules.dashboard.model.bo.DashboardTrendEventBO;
import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private DashboardMapper dashboardMapper;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(dashboardMapper);
        when(dashboardMapper.selectSummary()).thenReturn(com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardSummaryVO.builder()
                .userCount(10L)
                .articleCount(4L)
                .viewCount(100L)
                .likeCount(20L)
                .commentCount(6L)
                .messageCount(3L)
                .build());
        when(dashboardMapper.selectTrendBaselines(any())).thenReturn(List.of(
                new DashboardTrendEventBO(null, "users", 10L),
                new DashboardTrendEventBO(null, "publishedArticles", 4L),
                new DashboardTrendEventBO(null, "views", 100L),
                new DashboardTrendEventBO(null, "likes", 20L),
                new DashboardTrendEventBO(null, "comments", 6L),
                new DashboardTrendEventBO(null, "messages", 3L)
        ));
        when(dashboardMapper.selectTrendEvents(any(), any())).thenReturn(List.of(
                new DashboardTrendEventBO(LocalDate.now(), "users", 1L),
                new DashboardTrendEventBO(LocalDate.now(), "views", 5L),
                new DashboardTrendEventBO(LocalDate.now(), "likes", 2L)
        ));
        when(dashboardMapper.selectTopByViews()).thenReturn(List.of(new DashboardArticleRankBO(1L, "浏览最多", 20, 3, 2)));
        when(dashboardMapper.selectTopByLikes()).thenReturn(List.of(new DashboardArticleRankBO(2L, "点赞最多", 10, 8, 1)));
        when(dashboardMapper.selectTopByComments()).thenReturn(List.of(new DashboardArticleRankBO(3L, "评论最多", 9, 2, 7)));
        when(dashboardMapper.selectArticleCategoryDistribution()).thenReturn(List.of());
        when(dashboardMapper.selectArticleStatusDistribution()).thenReturn(List.of());
        when(dashboardMapper.selectArticleLikeActorDistribution()).thenReturn(List.of());
        when(dashboardMapper.selectCommentStatusDistribution()).thenReturn(List.of());
        when(dashboardMapper.selectMessageStatusDistribution()).thenReturn(List.of());
    }

    @Test
    void shouldBuildFourIndependentTrendGranularities() {
        DashboardVO result = dashboardService.getDashboard();

        assertEquals(10L, result.getSummary().getUserCount());
        assertEquals(30, result.getTrends().getUsers().getDay().size());
        assertEquals(12, result.getTrends().getUsers().getWeek().size());
        assertEquals(12, result.getTrends().getUsers().getMonth().size());
        assertEquals(5, result.getTrends().getUsers().getYear().size());
        assertEquals(11L, result.getTrends().getUsers().getDay().getLast().getValue());
        assertEquals(105L, result.getTrends().getViews().getDay().getLast().getValue());
        assertEquals(22L, result.getTrends().getLikes().getDay().getLast().getValue());
        assertEquals(1, result.getTopByViews().size());
        assertEquals("点赞最多", result.getTopByLikes().getFirst().getTitle());
    }
}
