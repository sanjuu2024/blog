<template>
	<div class="dashboard-panel dashboard-rank-panel">
		<div class="dashboard-panel__header">
			<h3>{{ title }}</h3>
		</div>
		<el-table
			:data="records"
			:show-header="false"
			:empty-text="`暂无${statLabel}数据`"
			class="dashboard-rank-table"
		>
			<el-table-column
				width="42"
				type="index"
			/>
			<el-table-column
				min-width="0"
				show-overflow-tooltip
			>
				<template #default="{ row }">
					<RouterLink
						class="dashboard-rank-link"
						:to="{ name: 'ArticleDetail', params: { articleId: row.id } }"
					>
						{{ row.title }}
					</RouterLink>
				</template>
			</el-table-column>
			<el-table-column
				width="72"
				align="right"
			>
				<template #default="{ row }">
					{{ formatNumber(row[statKey]) }}
				</template>
			</el-table-column>
		</el-table>
	</div>
</template>

<script setup lang="ts">
import type { DashboardArticleRank } from '../types/dashboard';
import { RouterLink } from 'vue-router';

defineOptions({
	name: 'DashboardRankTable',
});

defineProps<{
	title: string;
	records: DashboardArticleRank[];
	statKey: 'viewCount' | 'likeCount' | 'commentCount';
	statLabel: string;
}>();

function formatNumber(value: number) {
	return new Intl.NumberFormat('zh-CN').format(value);
}
</script>

<style scoped lang="scss">
.dashboard-panel {
	min-width: 0;
	overflow: hidden;
	border: 1px solid var(--app-border);
	border-radius: 0.5rem;
	background-color: var(--app-surface);
	padding: 1rem;
}

.dashboard-panel__header h3 {
	margin: 0;
	font-size: 1rem;
	font-weight: 700;
}

.dashboard-rank-table {
	width: 100%;
	min-width: 0;
	margin-top: 0.75rem;
}

.dashboard-rank-table :deep(.el-table__body-wrapper),
.dashboard-rank-table :deep(.el-scrollbar__wrap) {
	max-width: 100%;
	overflow-x: hidden;
}

.dashboard-rank-table :deep(.el-table__inner-wrapper::before) {
	display: none;
}

.dashboard-rank-link {
	display: block;
	overflow: hidden;
	color: var(--app-text);
	text-overflow: ellipsis;
	white-space: nowrap;
}

.dashboard-rank-link:hover {
	color: var(--el-color-primary);
}
</style>
