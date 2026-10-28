<template>
	<div class="dashboard-panel dashboard-trend-panel">
		<div class="dashboard-panel__header">
			<div class="dashboard-trend-panel__title">
				<h3>{{ title }}</h3>
				<el-radio-group
					v-model="mode"
					size="small"
					:aria-label="`${title}趋势模式`"
				>
					<el-radio-button value="increment">增量</el-radio-button>
					<el-radio-button value="cumulative">累积</el-radio-button>
				</el-radio-group>
			</div>
			<div class="dashboard-trend-panel__controls">
				<slot name="controls" />
			</div>
		</div>
		<VChart
			:theme="chartTheme"
			:option="chartOption"
			autoresize
			class="dashboard-trend-chart"
		/>
	</div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useTheme } from '@/composables/useTheme';
import { use } from 'echarts/core';
import { GridComponent, TooltipComponent } from 'echarts/components';
import { LineChart } from 'echarts/charts';
import { CanvasRenderer } from 'echarts/renderers';
import VChart from 'vue-echarts';
import type { DashboardTrendPoint } from '../types/dashboard';

use([CanvasRenderer, GridComponent, LineChart, TooltipComponent]);

defineOptions({
	name: 'DashboardTrendChart',
});

const props = defineProps<{
	title: string;
	points: DashboardTrendPoint[];
	granularity: 'day' | 'week' | 'month' | 'year';
}>();

const mode = ref<'increment' | 'cumulative'>('increment');
const { resolvedTheme } = useTheme();

const chartTheme = computed(() => (resolvedTheme.value === 'dark' ? 'dark' : 'vintage'));

function getCssColor(name: string, fallback: string) {
	if (typeof document === 'undefined') return fallback;
	return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}

const chartOption = computed(() => {
	void resolvedTheme.value;

	const textColor = getCssColor('--app-text-muted', '#8a8f98');
	const borderColor = getCssColor('--app-border', '#e5e7eb');
	const primaryColor = getCssColor('--el-color-primary', '#409eff');
	const surfaceColor = getCssColor('--app-surface', '#ffffff');
	const periods = props.points.map((point) => point.period);
	const values = props.points.map((point) =>
		mode.value === 'increment' ? point.increment : point.value,
	);
	const formatPeriod = (period: string) => {
		if (props.granularity === 'year') return period.slice(0, 4);
		if (props.granularity === 'month') return period.slice(0, 7);
		if (props.granularity === 'week') return period.slice(5, 10);
		return period.slice(5, 10);
	};
	const displayPeriods = periods.map(formatPeriod);

	return {
		animation: true,
		animationDuration: 700,
		animationEasing: 'cubicInOut' as const,
		animationDurationUpdate: 500,
		animationEasingUpdate: 'cubicInOut' as const,
		grid: { top: 18, right: 14, bottom: 28, left: 42, containLabel: false },
		tooltip: {
			trigger: 'axis',
			confine: true,
			formatter: (params: Array<{ axisValue: string; value: number }>) => {
				const point = params[0];
				return `${point.axisValue}<br/><strong>${new Intl.NumberFormat('zh-CN').format(point.value)}</strong>`;
			},
		},
		xAxis: {
			type: 'category',
			boundaryGap: false,
			data: displayPeriods,
			axisLine: { lineStyle: { color: borderColor } },
			axisTick: { show: false },
			axisLabel: {
				color: textColor,
				fontSize: 11,
				interval: periods.length > 12 ? Math.floor(periods.length / 5) : 0,
				hideOverlap: true,
			},
		},
		yAxis: {
			type: 'value',
			min: 0,
			minInterval: 1,
			axisLabel: {
				color: textColor,
				fontSize: 11,
				formatter: (value: number) => new Intl.NumberFormat('zh-CN').format(value),
			},
			axisLine: { show: false },
			axisTick: { show: false },
			splitLine: { lineStyle: { color: borderColor, type: 'dashed' } },
		},
		series: [
			{
				type: 'line',
				data: values,
				smooth: true,
				symbol: 'circle',
				symbolSize: 5,
				lineStyle: { width: 3, color: primaryColor },
				itemStyle: { color: primaryColor, borderColor: surfaceColor, borderWidth: 2 },
				areaStyle: {
					color: {
						type: 'linear',
						x: 0,
						y: 0,
						x2: 0,
						y2: 1,
						colorStops: [
							{ offset: 0, color: `${primaryColor}55` },
							{ offset: 1, color: `${primaryColor}08` },
						],
					},
				},
			},
		],
	};
});
</script>

<style scoped lang="scss">
.dashboard-panel {
	container: dashboard-panel / inline-size;
	border: 1px solid var(--app-border);
	border-radius: 0.5rem;
	background-color: var(--app-surface);
	padding: 1rem;
}

.dashboard-panel__header {
	display: grid;
	grid-template-columns: minmax(0, 1fr) auto;
	align-items: center;
	justify-content: space-between;
	gap: 1rem;
}

.dashboard-panel__header h3 {
	margin: 0;
	font-size: 1rem;
	font-weight: 700;
}

.dashboard-trend-panel__title {
	display: flex;
	min-width: 0;
	align-items: center;
	gap: 0.75rem;
}

.dashboard-trend-panel__title :deep(.el-radio-group) {
	flex: 0 0 auto;
}

.dashboard-trend-panel__controls {
	display: flex;
	min-width: 0;
	justify-content: flex-end;
}

.dashboard-trend-panel__controls :deep(.el-radio-group) {
	flex-wrap: nowrap;
}

.dashboard-trend-chart {
	min-width: 0;
	height: 17rem;
	margin-top: 0.5rem;
}

@container dashboard-panel (max-width: 320px) {
	.dashboard-panel__header {
		grid-template-columns: minmax(0, 1fr);
		gap: 0.75rem;
	}

	.dashboard-trend-panel__controls {
		justify-content: flex-start;
	}
}

@container dashboard-panel (max-width: 300px) {
	.dashboard-trend-panel__title {
		flex-wrap: wrap;
	}

	.dashboard-trend-panel__controls :deep(.el-radio-group) {
		flex-wrap: wrap;
	}
}

@media (width < 768px) {
	.dashboard-trend-chart {
		height: 14rem;
		min-width: 0;
	}
}
</style>
