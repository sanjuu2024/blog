import request from '@/utils/request';
import type {
	AdminNotificationItem,
	AdminNotificationListResponse,
	AdminNotificationResponse,
	CreateAdminNotificationRequest,
	NotificationStatus,
	UpdateAdminNotificationRequest,
} from '../types/adminNotification';

const ADMIN_NOTIFICATION_API = {
	list: 'admin/notifications',
	status: (id: number) => `admin/notifications/${id}/status`,
	item: (id: number) => `admin/notifications/${id}`,
} as const;

// 获取管理员消息列表接口
export const listAdminNotifications = (): Promise<AdminNotificationItem[]> => {
	// 🔺request.get<后端原始响应, 拦截器最终返回值, 请求体类型>(url)
	return request.get<AdminNotificationListResponse, AdminNotificationItem[], null>(
		ADMIN_NOTIFICATION_API.list,
	);
};

// 创建管理员消息接口
export const createAdminNotification = (
	data: CreateAdminNotificationRequest,
): Promise<AdminNotificationItem> => {
	// 🔺request.post<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data)
	return request.post<
		AdminNotificationResponse,
		AdminNotificationItem,
		CreateAdminNotificationRequest
	>(ADMIN_NOTIFICATION_API.list, data);
};

// 更新管理员消息草稿接口
export const updateAdminNotification = (
	id: number,
	data: UpdateAdminNotificationRequest,
): Promise<AdminNotificationItem> => {
	// 🔺request.put<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data)
	return request.put<
		AdminNotificationResponse,
		AdminNotificationItem,
		UpdateAdminNotificationRequest
	>(ADMIN_NOTIFICATION_API.item(id), data);
};

// 修改管理员消息状态接口
export const updateAdminNotificationStatus = (
	id: number,
	status: NotificationStatus,
): Promise<AdminNotificationItem> => {
	// 🔺注意 status 是 query 参数，所以通过 request.patch() 的第三个配置参数传递。
	// 🔺request.patch<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data, config)
	return request.patch<AdminNotificationResponse, AdminNotificationItem, null>(
		ADMIN_NOTIFICATION_API.status(id),
		null,
		{ params: { status } },
	);
};
