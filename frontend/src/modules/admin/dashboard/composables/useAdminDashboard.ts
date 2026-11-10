import { getDashboard } from '../api/dashboardApi';
import type { DashboardData } from '../types/dashboard';

export function useAdminDashboard() {
	const dashboard = ref<DashboardData | null>(null);
	const loading = ref(false);
	const loadFailed = ref(false);

	// 获取 Dashboard 数据
	async function getAdminDashboard() {
		if (loading.value) return false;

		loading.value = true;
		loadFailed.value = false;
		try {
			dashboard.value = await getDashboard();
			return true;
		} catch {
			// 请求错误由统一响应拦截器提示。
			loadFailed.value = true;
			return false;
		} finally {
			loading.value = false;
		}
	}

	return {
		dashboard,
		loading,
		loadFailed,
		getAdminDashboard,
	};
}
