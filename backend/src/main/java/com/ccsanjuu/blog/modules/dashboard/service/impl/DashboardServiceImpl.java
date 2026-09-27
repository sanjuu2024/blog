package com.ccsanjuu.blog.modules.dashboard.service.impl;

import com.ccsanjuu.blog.modules.dashboard.mapper.DashboardMapper;
import com.ccsanjuu.blog.modules.dashboard.model.bo.DashboardArticleRankBO;
import com.ccsanjuu.blog.modules.dashboard.model.bo.DashboardBreakdownBO;
import com.ccsanjuu.blog.modules.dashboard.model.bo.DashboardTrendEventBO;
import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardArticleRankVO;
import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardBreakdownVO;
import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardMetricTrendVO;
import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardSummaryVO;
import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardTrendPointVO;
import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardTrendsVO;
import com.ccsanjuu.blog.modules.dashboard.model.vo.DashboardVO;
import com.ccsanjuu.blog.modules.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String USERS = "users";
    private static final String PUBLISHED_ARTICLES = "publishedArticles";
    private static final String VIEWS = "views";
    private static final String LIKES = "likes";
    private static final String COMMENTS = "comments";
    private static final String MESSAGES = "messages";

    private final DashboardMapper dashboardMapper;

    /**
     * 获取管理员 Dashboard 数据。
     *
     * @return Dashboard 总量、趋势和文章排名
     */
    @Override
    @Transactional(readOnly = true)
    public DashboardVO getDashboard() {
        LocalDate today = LocalDate.now(DASHBOARD_ZONE);
        LocalDate trendStartDate = getTrendStartDate(today);
        LocalDate endDateExclusive = today.plusDays(1);

        List<DashboardTrendEventBO> baselineRows = dashboardMapper.selectTrendBaselines(trendStartDate);
        List<DashboardTrendEventBO> eventRows = dashboardMapper.selectTrendEvents(
                trendStartDate,
                endDateExclusive
        );

        Map<String, Long> baselines = toBaselineMap(baselineRows);
        Map<String, Map<LocalDate, Long>> events = toEventMap(eventRows);

        return DashboardVO.builder()
                .summary(dashboardMapper.selectSummary())
                .trends(DashboardTrendsVO.builder()
                        .users(buildMetricTrend(USERS, today, baselines, events))
                        .publishedArticles(buildMetricTrend(PUBLISHED_ARTICLES, today, baselines, events))
                        .views(buildMetricTrend(VIEWS, today, baselines, events))
                        .likes(buildMetricTrend(LIKES, today, baselines, events))
                        .comments(buildMetricTrend(COMMENTS, today, baselines, events))
                        .messages(buildMetricTrend(MESSAGES, today, baselines, events))
                        .build())
                .topByViews(toArticleRankVOList(dashboardMapper.selectTopByViews()))
                .topByLikes(toArticleRankVOList(dashboardMapper.selectTopByLikes()))
                .topByComments(toArticleRankVOList(dashboardMapper.selectTopByComments()))
                .articleCategoryDistribution(toBreakdownVOList(dashboardMapper.selectArticleCategoryDistribution()))
                .articleStatusDistribution(toBreakdownVOList(dashboardMapper.selectArticleStatusDistribution()))
                .articleLikeActorDistribution(toBreakdownVOList(dashboardMapper.selectArticleLikeActorDistribution()))
                .commentStatusDistribution(toStatusBreakdownVOList(
                        dashboardMapper.selectCommentStatusDistribution(),
                        List.of("PENDING", "APPROVED", "REJECTED", "HIDDEN", "DELETED")
                ))
                .messageStatusDistribution(toStatusBreakdownVOList(
                        dashboardMapper.selectMessageStatusDistribution(),
                        List.of("PENDING", "APPROVED", "REJECTED", "HIDDEN", "DELETED")
                ))
                .build();
    }

    /**
     * 计算四种趋势粒度所需的最早日期。
     *
     * @param today 当前自然日
     * @return 趋势窗口最早日期
     */
    private LocalDate getTrendStartDate(LocalDate today) {
        LocalDate weekStart = getWeekStart(today).minusWeeks(11);
        LocalDate monthStart = today.withDayOfMonth(1).minusMonths(11);
        LocalDate yearStart = today.withDayOfYear(1).minusYears(4);
        return LocalDate.ofEpochDay(
                Math.min(
                        Math.min(today.minusDays(29).toEpochDay(), weekStart.toEpochDay()),
                        Math.min(monthStart.toEpochDay(), yearStart.toEpochDay())
                )
        );
    }

    /**
     * 组装指定指标的日、周、月、年累计趋势。
     *
     * @param metric 指标名称
     * @param today 当前自然日
     * @param baselines 起始日期前的累计值
     * @param events 起始日期后的事件数据
     * @return 指标趋势
     */
    private DashboardMetricTrendVO buildMetricTrend(
            String metric,
            LocalDate today,
            Map<String, Long> baselines,
            Map<String, Map<LocalDate, Long>> events
    ) {
        Map<LocalDate, Long> metricEvents = events.getOrDefault(metric, Map.of());
        long baseline = baselines.getOrDefault(metric, 0L);
        return DashboardMetricTrendVO.builder()
                .day(buildTrendPoints(metricEvents, baseline, today.minusDays(29), 30, Period.DAY, today))
                .week(buildTrendPoints(metricEvents, baseline, getWeekStart(today).minusWeeks(11), 12, Period.WEEK, today))
                .month(buildTrendPoints(metricEvents, baseline, today.withDayOfMonth(1).minusMonths(11), 12, Period.MONTH, today))
                .year(buildTrendPoints(metricEvents, baseline, today.withDayOfYear(1).minusYears(4), 5, Period.YEAR, today))
                .build();
    }

    /**
     * 按指定粒度生成累计趋势点。
     *
     * @param events 事件数据
     * @param baseline 窗口开始前的累计值
     * @param firstPeriodStart 第一个统计周期起点
     * @param periodCount 周期数量
     * @param period 粒度
     * @param today 当前自然日
     * @return 趋势点列表
     */
    private List<DashboardTrendPointVO> buildTrendPoints(
            Map<LocalDate, Long> events,
            long baseline,
            LocalDate firstPeriodStart,
            int periodCount,
            Period period,
            LocalDate today
    ) {
        List<DashboardTrendPointVO> points = new ArrayList<>(periodCount);
        LocalDate cursorDate = events.keySet().stream()
                .min(LocalDate::compareTo)
                .orElse(firstPeriodStart);
        long cumulative = baseline;

        for (int index = 0; index < periodCount; index++) {
            LocalDate periodStart = addPeriods(firstPeriodStart, index, period);
            LocalDate periodEnd = addPeriods(firstPeriodStart, index + 1, period).minusDays(1);
            periodEnd = periodEnd.isAfter(today) ? today : periodEnd;

            while (!cursorDate.isAfter(periodEnd)) {
                cumulative += events.getOrDefault(cursorDate, 0L);
                cursorDate = cursorDate.plusDays(1);
            }

            points.add(DashboardTrendPointVO.builder()
                    .period(periodStart.toString())
                    .value(cumulative)
                    .build());
        }
        return points;
    }

    /**
     * 根据粒度推进统计周期。
     *
     * @param start 起始周期
     * @param amount 推进数量
     * @param period 粒度
     * @return 推进后的周期
     */
    private LocalDate addPeriods(LocalDate start, int amount, Period period) {
        return switch (period) {
            case DAY -> start.plusDays(amount);
            case WEEK -> start.plusWeeks(amount);
            case MONTH -> start.plusMonths(amount);
            case YEAR -> start.plusYears(amount);
        };
    }

    /**
     * 将事件列表转换为按指标和日期索引的 Map。
     *
     * @param rows 事件列表
     * @return 事件 Map
     */
    private Map<String, Map<LocalDate, Long>> toEventMap(List<DashboardTrendEventBO> rows) {
        Map<String, Map<LocalDate, Long>> result = new HashMap<>();
        rows.forEach(row -> result
                .computeIfAbsent(row.getMetric(), key -> new HashMap<>())
                .put(row.getEventDate(), row.getAmount()));
        return result;
    }

    /**
     * 将趋势基数列表转换为按指标索引的 Map。
     *
     * @param rows 基数列表
     * @return 基数 Map
     */
    private Map<String, Long> toBaselineMap(List<DashboardTrendEventBO> rows) {
        Map<String, Long> result = new HashMap<>();
        rows.forEach(row -> result.put(row.getMetric(), row.getAmount()));
        return result;
    }

    /**
     * 转换文章排名结果。
     *
     * @param records 文章排名 BO
     * @return 文章排名 VO
     */
    private List<DashboardArticleRankVO> toArticleRankVOList(List<DashboardArticleRankBO> records) {
        return records.stream()
                .map(record -> DashboardArticleRankVO.builder()
                        .id(record.getId())
                        .title(record.getTitle())
                        .viewCount(record.getViewCount())
                        .likeCount(record.getLikeCount())
                        .commentCount(record.getCommentCount())
                        .build())
                .toList();
    }

    /**
     * 转换 Dashboard 分布结果。
     *
     * @param records 分布结果
     * @return 分布响应
     */
    private List<DashboardBreakdownVO> toBreakdownVOList(List<DashboardBreakdownBO> records) {
        return records == null ? List.of() : records.stream()
                .map(record -> DashboardBreakdownVO.builder()
                        .key(record.getKey())
                        .label(record.getLabel())
                        .value(record.getValue())
                        .build())
                .toList();
    }

    /**
     * 补齐审核状态分布中的零值状态，保证堆叠图例稳定。
     *
     * @param records 数据库返回的状态分布
     * @param statusKeys 状态顺序
     * @return 补齐后的状态分布
     */
    private List<DashboardBreakdownVO> toStatusBreakdownVOList(
            List<DashboardBreakdownBO> records,
            List<String> statusKeys
    ) {
        Map<String, DashboardBreakdownBO> recordMap = records == null ? Map.of() : records.stream()
                .collect(java.util.stream.Collectors.toMap(DashboardBreakdownBO::getKey, record -> record));
        return statusKeys.stream()
                .map(key -> {
                    DashboardBreakdownBO record = recordMap.get(key);
                    return DashboardBreakdownVO.builder()
                            .key(key)
                            .label(key)
                            .value(record == null ? 0L : record.getValue())
                            .build();
                })
                .toList();
    }

    /**
     * 获取当前自然日所属周的周一。
     *
     * @param date 日期
     * @return 周一日期
     */
    private LocalDate getWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private enum Period {
        DAY,
        WEEK,
        MONTH,
        YEAR
    }
}
