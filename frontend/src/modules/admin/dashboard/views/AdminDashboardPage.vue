<template>
	<div class="admin-dashboard">
		<div class="admin-dashboard__header">
			<h1>Dashboard</h1>
			<el-button
				:loading="loading"
				aria-label="刷新 Dashboard"
				@click="getAdminDashboard"
			>
				<i-lucide-refresh-cw class="mr-1" />
				刷新
			</el-button>
		</div>

		<div
			v-if="loadFailed"
			class="admin-dashboard__state"
		>
			<p>Dashboard 数据加载失败</p>
			<el-button
				type="primary"
				@click="getAdminDashboard"
			>
				重试
			</el-button>
		</div>

		<AppLoading
			v-else
			:loading="loading"
		>
			<template v-if="dashboard">
				<section class="dashboard-summary-grid">
					<div
						v-for="item in summaryItems"
						:key="item.key"
						class="dashboard-summary-item"
					>
						<div class="dashboard-summary-item__icon">
							<component :is="item.icon" />
						</div>
						<div>
							<p>{{ item.label }}</p>
							<strong class="text-2xl font-bold">{{
								formatNumber(dashboard?.summary[item.key] ?? 0)
							}}</strong>
						</div>
					</div>
				</section>

				<section class="dashboard-section">
					<div class="dashboard-section__header">
						<h2>趋势</h2>
						<span>累计总数</span>
					</div>
					<div class="dashboard-trend-grid">
						<DashboardTrendChart
							v-for="item in trendItems"
							:key="item.key"
							:title="item.label"
							:points="getTrendPoints(item.key)"
							:granularity="granularityMap[item.key]"
						>
							<template #controls>
								<el-radio-group
									v-model="granularityMap[item.key]"
									size="small"
								>
									<el-radio-button label="day">日</el-radio-button>
									<el-radio-button label="week">周</el-radio-button>
									<el-radio-button label="month">月</el-radio-button>
									<el-radio-button label="year">年</el-radio-button>
								</el-radio-group>
							</template>
						</DashboardTrendChart>
					</div>
				</section>

				<section class="dashboard-section">
					<div class="dashboard-section__header">
						<h2>分布</h2>
						<span>当前数据</span>
					</div>
					<div class="dashboard-distribution-grid">
						<DashboardBreakdownChart
							title="文章一级分类占比"
							:items="dashboard?.articleCategoryDistribution ?? []"
						/>
						<DashboardBreakdownChart
							title="文章状态分布"
							:items="dashboard?.articleStatusDistribution ?? []"
						/>
						<DashboardBreakdownChart
							title="文章点赞主体分布"
							:items="dashboard?.articleLikeActorDistribution ?? []"
						/>
					</div>
					<div class="mt-3">
						<DashboardModerationChart
							:comments="dashboard?.commentStatusDistribution ?? []"
							:messages="dashboard?.messageStatusDistribution ?? []"
						/>
					</div>
				</section>

				<section class="dashboard-section">
					<div class="dashboard-section__header">
						<h2>文章排名</h2>
					</div>
					<div class="dashboard-ranking-grid">
						<DashboardRankTable
							title="浏览 Top 10"
							:records="dashboard?.topByViews ?? []"
							stat-key="viewCount"
							stat-label="浏览"
						/>
						<DashboardRankTable
							title="点赞 Top 10"
							:records="dashboard?.topByLikes ?? []"
							stat-key="likeCount"
							stat-label="点赞"
						/>
						<DashboardRankTable
							title="评论 Top 10"
							:records="dashboard?.topByComments ?? []"
							stat-key="commentCount"
							stat-label="评论"
						/>
					</div>
				</section>
			</template>
		</AppLoading>
	</div>
</template>

<script setup lang="ts">
import { onMounted, reactive } from 'vue';
import IconUsers from '~icons/lucide/users';
import DashboardRankTable from '../components/DashboardRankTable.vue';
import DashboardBreakdownChart from '../components/DashboardBreakdownChart.vue';
import DashboardModerationChart from '../components/DashboardModerationChart.vue';
import DashboardTrendChart from '../components/DashboardTrendChart.vue';
import { useAdminDashboard } from '../composables/useAdminDashboard';
import { DASHBOARD_GRANULARITY, type DashboardGranularity } from '../types/dashboard';
import getRouteIcon from '@/utils/getRouteIcon.ts';

defineOptions({
	name: 'AdminDashboardPage',
});

const summaryItems = [
	{ key: 'userCount', label: '用户', icon: IconUsers },
	{ key: 'articleCount', label: '文章', icon: getRouteIcon('article') },
	{ key: 'viewCount', label: '浏览', icon: getRouteIcon('view') },
	{ key: 'likeCount', label: '点赞', icon: getRouteIcon('like') },
	{ key: 'commentCount', label: '评论', icon: getRouteIcon('comment') },
	{ key: 'messageCount', label: '留言', icon: getRouteIcon('message') },
] as const;

const trendItems = [
	{ key: 'users', label: '用户' },
	{ key: 'publishedArticles', label: '文章' },
	{ key: 'views', label: '浏览' },
	{ key: 'likes', label: '点赞' },
	{ key: 'comments', label: '评论' },
	{ key: 'messages', label: '留言' },
] as const;

type TrendKey = (typeof trendItems)[number]['key'];

const { dashboard, loading, loadFailed, getAdminDashboard } = useAdminDashboard();

const granularityMap = reactive<Record<TrendKey, DashboardGranularity>>({
	users: DASHBOARD_GRANULARITY.DAY,
	publishedArticles: DASHBOARD_GRANULARITY.DAY,
	views: DASHBOARD_GRANULARITY.DAY,
	likes: DASHBOARD_GRANULARITY.DAY,
	comments: DASHBOARD_GRANULARITY.DAY,
	messages: DASHBOARD_GRANULARITY.DAY,
});

function getTrendPoints(key: TrendKey) {
	if (!dashboard.value) return [];
	return dashboard.value.trends[key][granularityMap[key]];
}

function formatNumber(value: number) {
	return new Intl.NumberFormat('zh-CN').format(value);
}

onMounted(() => {
	getAdminDashboard();
});
</script>

<style scoped lang="scss">
.admin-dashboard {
	height: 100%;
	overflow-y: auto;
	padding: 1rem;
}

.admin-dashboard__header {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.dashboard-section {
	margin-top: 2.5rem;
}

.admin-dashboard__header,
.dashboard-section__header {
	margin-bottom: 1rem;
}

.admin-dashboard__header h1,
.dashboard-section__header h2,
.dashboard-panel__header h3 {
	font-weight: bold;
}

.admin-dashboard__header h1 {
	font-size: 1.5rem;
}

.dashboard-section__header h2 {
	font-size: 1.5rem;
}

.dashboard-section__header span {
	color: var(--app-text-muted);
	font-size: 0.9rem;
}

.dashboard-summary-grid,
.dashboard-trend-grid,
.dashboard-distribution-grid,
.dashboard-ranking-grid {
	display: grid;
	gap: 0.75rem;
	min-width: 0;
}

.dashboard-summary-grid > *,
.dashboard-trend-grid > *,
.dashboard-distribution-grid > *,
.dashboard-ranking-grid > * {
	min-width: 0;
}

.dashboard-summary-grid {
	grid-template-columns: repeat(6, minmax(0, 1fr));
}

.dashboard-trend-grid {
	grid-template-columns: repeat(2, minmax(0, 1fr));
}

.dashboard-distribution-grid {
	grid-template-columns: repeat(3, minmax(0, 1fr));
}

.dashboard-ranking-grid {
	grid-template-columns: repeat(3, minmax(0, 1fr));
}

.dashboard-summary-item {
	display: flex;
	align-items: center;
	gap: 0.75rem;
	min-width: 0;
	padding: 1rem;
	border: 1px solid var(--app-border);
	border-radius: 0.5rem;
	background-color: var(--app-surface);
}

.dashboard-summary-item__icon {
	display: grid;
	width: 2.5rem;
	height: 2.5rem;
	flex: 0 0 auto;
	place-items: center;
	border-radius: 0.5rem;
	background-color: var(--app-router-hover);
	color: var(--el-color-primary);
	font-size: 1.25rem;
}

.admin-dashboard__state {
	display: flex;
	min-height: 16rem;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 1rem;
	color: var(--app-text-muted);
}

@media (width < 1200px) {
	.dashboard-summary-grid {
		grid-template-columns: repeat(3, minmax(0, 1fr));
	}

	.dashboard-ranking-grid {
		grid-template-columns: 1fr;
	}

	.dashboard-distribution-grid {
		grid-template-columns: repeat(2, minmax(0, 1fr));
	}
}

@media (width < 768px) {
	.dashboard-trend-grid {
		grid-template-columns: 1fr;
	}
}

@media (width < 500px) {
	.dashboard-summary-grid,
	.dashboard-distribution-grid {
		grid-template-columns: repeat(2, minmax(0, 1fr));
	}
}

@media (width < 400px) {
	.dashboard-distribution-grid {
		grid-template-columns: 1fr;
	}
}
</style>
