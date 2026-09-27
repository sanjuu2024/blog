<template>
	<div class="dashboard-panel dashboard-moderation-panel">
		<div class="dashboard-panel__header">
			<h3>评论与留言审核状态</h3>
			<p>按当前记录状态统计</p>
		</div>
		<VChart
			:theme="chartTheme"
			:option="chartOption"
			autoresize
			class="dashboard-moderation-chart"
		/>
	</div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { use } from 'echarts/core';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { BarChart } from 'echarts/charts';
import { CanvasRenderer } from 'echarts/renderers';
import VChart from 'vue-echarts';
import { useTheme } from '@/composables/useTheme';
import type { DashboardBreakdownItem } from '../types/dashboard';

use([BarChart, CanvasRenderer, GridComponent, LegendComponent, TooltipComponent]);

defineOptions({ name: 'DashboardModerationChart' });

const props = defineProps<{
	comments: DashboardBreakdownItem[];
	messages: DashboardBreakdownItem[];
}>();

const { resolvedTheme } = useTheme();

const chartTheme = computed(() => (resolvedTheme.value === 'dark' ? 'dark' : 'vintage'));

const statusMeta = [
	{ key: 'PENDING', label: '待审核', color: '#c58a3a' },
	{ key: 'APPROVED', label: '已通过', color: '#5b8f63' },
	{ key: 'REJECTED', label: '已拒绝', color: '#bd6657' },
	{ key: 'HIDDEN', label: '已隐藏', color: '#8a8f98' },
	{ key: 'DELETED', label: '已删除', color: '#5d6470' },
] as const;

const chartOption = computed(() => {
	void resolvedTheme.value;
	const textColor = getComputedStyle(document.documentElement)
		.getPropertyValue('--app-text-muted')
		.trim();
	const borderColor = getComputedStyle(document.documentElement)
		.getPropertyValue('--app-border')
		.trim();
	const commentMap = new Map(props.comments.map((item) => [item.key, item.value]));
	const messageMap = new Map(props.messages.map((item) => [item.key, item.value]));

	return {
		animation: true,
		animationDuration: 700,
		animationEasing: 'cubicInOut' as const,
		grid: { top: 28, right: 16, bottom: 24, left: 52, containLabel: false },
		tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
		legend: { top: 0, textStyle: { color: textColor || '#8a8f98', fontSize: 12 } },
		xAxis: {
			type: 'value',
			minInterval: 1,
			axisLabel: { color: textColor || '#8a8f98' },
			axisLine: { show: false },
			axisTick: { show: false },
			splitLine: { lineStyle: { color: borderColor || '#e5e7eb', type: 'dashed' } },
		},
		yAxis: {
			type: 'category',
			data: ['评论', '留言'],
			axisLabel: { color: textColor || '#8a8f98' },
			axisLine: { show: false },
			axisTick: { show: false },
		},
		series: statusMeta.map((status) => ({
			name: status.label,
			type: 'bar',
			stack: 'total',
			barMaxWidth: 34,
			itemStyle: { color: status.color },
			data: [commentMap.get(status.key) ?? 0, messageMap.get(status.key) ?? 0],
		})),
	};
});
</script>

<style scoped lang="scss">
.dashboard-panel {
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

.dashboard-panel__header p {
	margin: 0.25rem 0 0;
	color: var(--app-text-muted);
	font-size: 0.78rem;
}

.dashboard-moderation-chart {
	width: 100%;
	height: 17rem;
	margin-top: 0.25rem;
}
</style>
