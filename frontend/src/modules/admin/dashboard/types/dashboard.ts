import type { ApiResult } from '@/types/api';

export const DASHBOARD_GRANULARITY = {
	DAY: 'day',
	WEEK: 'week',
	MONTH: 'month',
	YEAR: 'year',
} as const;

export type DashboardGranularity =
	(typeof DASHBOARD_GRANULARITY)[keyof typeof DASHBOARD_GRANULARITY];

export interface DashboardSummary {
	userCount: number;
	articleCount: number;
	viewCount: number;
	likeCount: number;
	commentCount: number;
	messageCount: number;
}

export interface DashboardTrendPoint {
	period: string;
	value: number;
}

export interface DashboardMetricTrend {
	day: DashboardTrendPoint[];
	week: DashboardTrendPoint[];
	month: DashboardTrendPoint[];
	year: DashboardTrendPoint[];
}

export interface DashboardTrends {
	users: DashboardMetricTrend;
	publishedArticles: DashboardMetricTrend;
	views: DashboardMetricTrend;
	likes: DashboardMetricTrend;
	comments: DashboardMetricTrend;
	messages: DashboardMetricTrend;
}

export interface DashboardArticleRank {
	id: number;
	title: string;
	viewCount: number;
	likeCount: number;
	commentCount: number;
}

export interface DashboardBreakdownItem {
	key: string;
	label: string;
	value: number;
}

export interface DashboardData {
	summary: DashboardSummary;
	trends: DashboardTrends;
	topByViews: DashboardArticleRank[];
	topByLikes: DashboardArticleRank[];
	topByComments: DashboardArticleRank[];
	articleCategoryDistribution: DashboardBreakdownItem[];
	articleStatusDistribution: DashboardBreakdownItem[];
	articleLikeActorDistribution: DashboardBreakdownItem[];
	commentStatusDistribution: DashboardBreakdownItem[];
	messageStatusDistribution: DashboardBreakdownItem[];
}

export type DashboardResponse = ApiResult<DashboardData>;
