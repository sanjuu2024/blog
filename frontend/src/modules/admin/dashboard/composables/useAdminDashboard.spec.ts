import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getDashboard } from '../api/dashboardApi';
import type { DashboardData } from '../types/dashboard';
import { useAdminDashboard } from './useAdminDashboard';

vi.mock('../api/dashboardApi', () => ({
	getDashboard: vi.fn(),
}));

const dashboard: DashboardData = {
	summary: {
		userCount: 10,
		articleCount: 4,
		viewCount: 100,
		likeCount: 20,
		commentCount: 6,
		messageCount: 3,
	},
	trends: {} as DashboardData['trends'],
	topByViews: [],
	topByLikes: [],
	topByComments: [],
	articleCategoryDistribution: [],
	articleStatusDistribution: [],
	articleLikeActorDistribution: [],
	commentStatusDistribution: [],
	messageStatusDistribution: [],
};

describe('useAdminDashboard', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('loads dashboard data and prevents duplicate requests', async () => {
		let resolveRequest!: (value: DashboardData) => void;
		vi.mocked(getDashboard).mockReturnValue(
			new Promise((resolve) => {
				resolveRequest = resolve;
			}),
		);

		const state = useAdminDashboard();
		const firstRequest = state.getAdminDashboard();
		const secondRequest = state.getAdminDashboard();

		expect(getDashboard).toHaveBeenCalledTimes(1);
		resolveRequest(dashboard);
		await Promise.all([firstRequest, secondRequest]);

		expect(state.dashboard.value).toEqual(dashboard);
		expect(state.loadFailed.value).toBe(false);
	});

	it('marks the page as failed when loading fails', async () => {
		vi.mocked(getDashboard).mockRejectedValue(new Error('request failed'));

		const state = useAdminDashboard();
		await state.getAdminDashboard();

		expect(state.loadFailed.value).toBe(true);
	});
});
