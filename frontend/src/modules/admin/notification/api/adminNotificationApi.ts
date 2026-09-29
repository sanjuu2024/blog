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

export const listAdminNotifications = (): Promise<AdminNotificationItem[]> => {
	return request.get<AdminNotificationListResponse, AdminNotificationItem[], null>(
		ADMIN_NOTIFICATION_API.list,
	);
};

export const createAdminNotification = (
	data: CreateAdminNotificationRequest,
): Promise<AdminNotificationItem> => {
	return request.post<
		AdminNotificationResponse,
		AdminNotificationItem,
		CreateAdminNotificationRequest
	>(ADMIN_NOTIFICATION_API.list, data);
};

export const updateAdminNotification = (
	id: number,
	data: UpdateAdminNotificationRequest,
): Promise<AdminNotificationItem> => {
	return request.put<
		AdminNotificationResponse,
		AdminNotificationItem,
		UpdateAdminNotificationRequest
	>(ADMIN_NOTIFICATION_API.item(id), data);
};

export const updateAdminNotificationStatus = (
	id: number,
	status: NotificationStatus,
): Promise<AdminNotificationItem> => {
	return request.patch<AdminNotificationResponse, AdminNotificationItem, null>(
		ADMIN_NOTIFICATION_API.status(id),
		null,
		{ params: { status } },
	);
};
