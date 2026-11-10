<template>
	<div class="dashboard-panel dashboard-breakdown-panel">
		<div class="dashboard-panel__header">
			<div>
				<h3>{{ title }}</h3>
				<p v-if="note">{{ note }}</p>
			</div>
		</div>
		<VChart
			:theme="chartTheme"
			:option="chartOption"
			autoresize
			class="dashboard-breakdown-chart"
		/>
	</div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { use } from 'echarts/core';
import { LegendComponent, TooltipComponent } from 'echarts/components';
import { PieChart } from 'echarts/charts';
import { CanvasRenderer } from 'echarts/renderers';
import VChart from 'vue-echarts';
import { useTheme } from '@/composables/useTheme';
import type { DashboardBreakdownItem } from '../types/dashboard';

use([CanvasRenderer, LegendComponent, PieChart, TooltipComponent]);

defineOptions({ name: 'DashboardBreakdownChart' });

const props = defineProps<{
	title: string;
	note?: string;
	items: DashboardBreakdownItem[];
}>();

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

	return {
		animation: true,
		animationDuration: 700,
		animationEasing: 'cubicInOut' as const,
		tooltip: { trigger: 'item', formatter: '{b}<br/>{c} ({d}%)' },
		legend: {
			bottom: 0,
			left: 'center',
			icon: 'roundRect',
			textStyle: { color: textColor, fontSize: 12 },
			itemWidth: 10,
			itemHeight: 10,
		},
		series: [
			{
				type: 'pie',
				radius: ['48%', '72%'],
				center: ['50%', '45%'],
				avoidLabelOverlap: true,
				itemStyle: { borderColor, borderWidth: 2 },
				label: { show: false },
				emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
				data: props.items.map((item) => ({ name: item.label, value: item.value })),
			},
		],
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

.dashboard-breakdown-chart {
	width: 100%;
	height: 17rem;
	margin-top: 0.25rem;
}
</style>
