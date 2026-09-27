import request from '@/utils/request';
import type { DashboardData, DashboardResponse } from '../types/dashboard';

const DASHBOARD_API = {
	getDashboard: 'admin/dashboard',
} as const;

// 获取管理员 Dashboard 数据
export const getDashboard = (): Promise<DashboardData> => {
	return request.get<DashboardResponse, DashboardData, null>(DASHBOARD_API.getDashboard);
};
