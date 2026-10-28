import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import DashboardTrendChart from './DashboardTrendChart.vue';

vi.mock('echarts/core', () => ({
	use: vi.fn(),
}));

vi.mock('vue-echarts', () => ({
	default: {
		name: 'VChart',
		props: ['option', 'theme'],
		template: '<div data-test="chart" />',
	},
}));

const points = [
	{ period: '2026-10-27', increment: 3, value: 40 },
	{ period: '2026-10-28', increment: 2, value: 42 },
];

function getSeriesValues(wrapper: ReturnType<typeof mount>) {
	const option = wrapper.findComponent({ name: 'VChart' }).props('option') as {
		series: Array<{ data: number[] }>;
	};
	return option.series[0].data;
}

describe('DashboardTrendChart', () => {
	it('shows period increments by default and switches to cumulative values', async () => {
		const wrapper = mount(DashboardTrendChart, {
			props: {
				title: '浏览',
				points,
				granularity: 'day',
			},
		});

		expect(getSeriesValues(wrapper)).toEqual([3, 2]);

		await wrapper.findAll('input[type="radio"]')[1].setValue(true);

		expect(getSeriesValues(wrapper)).toEqual([40, 42]);
	});

	it('keeps the selected mode independent for each chart instance', async () => {
		const options = {
			props: {
				title: '浏览',
				points,
				granularity: 'day' as const,
			},
		};
		const first = mount(DashboardTrendChart, options);
		const second = mount(DashboardTrendChart, {
			...options,
			props: { ...options.props, title: '点赞' },
		});

		await first.findAll('input[type="radio"]')[1].setValue(true);

		expect(getSeriesValues(first)).toEqual([40, 42]);
		expect(getSeriesValues(second)).toEqual([3, 2]);
	});
});
