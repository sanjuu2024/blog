import request from '@/utils/request';
import type { AxiosRequestConfig } from 'axios';
import type {
	NotificationCategory,
	NotificationListQuery,
	NotificationMutationResponse,
	NotificationPageData,
	NotificationPageResponse,
	NotificationUnreadCount,
	NotificationUnreadCountResponse,
} from '../types/notification';

const NOTIFICATION_API = {
	list: 'notifications',
	count: 'notifications/unread-count',
	markRead: (notificationId: number) => `notifications/${notificationId}/read`,
	markAllRead: 'notifications/read-all',
} as const;

export const listNotifications = (
	params?: NotificationListQuery,
): Promise<NotificationPageData> => {
	return request.get<NotificationPageResponse, NotificationPageData, NotificationListQuery>(
		NOTIFICATION_API.list,
		{ params },
	);
};

export const getUnreadCount = (): Promise<NotificationUnreadCount> => {
	return request.get<NotificationUnreadCountResponse, NotificationUnreadCount, null>(
		NOTIFICATION_API.count,
	);
};

export const markNotificationRead = (notificationId: number): Promise<null> => {
	return request.patch<NotificationMutationResponse, null, null>(
		NOTIFICATION_API.markRead(notificationId),
	);
};

export const markAllNotificationsRead = (category?: NotificationCategory): Promise<null> => {
	const config: Omit<AxiosRequestConfig, 'data'> = {
		params: category && category !== 'ALL' ? { category } : undefined,
	};
	return request.patch<NotificationMutationResponse, null, null>(
		NOTIFICATION_API.markAllRead,
		null,
		config,
	);
};
