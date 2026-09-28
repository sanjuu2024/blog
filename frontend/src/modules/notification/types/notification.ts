import type { ApiResult, PageResult } from '@/types/api';

export const NOTIFICATION_CATEGORY = {
	ALL: 'ALL',
	REPLY: 'REPLY',
	ADMIN_MESSAGE: 'ADMIN_MESSAGE',
} as const;

export type NotificationCategory =
	(typeof NOTIFICATION_CATEGORY)[keyof typeof NOTIFICATION_CATEGORY];

export type NotificationType = 'COMMENT_REPLY' | 'MESSAGE_REPLY' | 'ADMIN_MESSAGE';

export interface NotificationItem {
	id: number;
	type: NotificationType;
	title: string;
	content: string;
	read: boolean;
	createdAt: string;
}

export interface NotificationListQuery {
	pageNum?: number;
	pageSize?: number;
	category?: NotificationCategory;
}

export interface NotificationUnreadCount {
	total: number;
	reply: number;
	adminMessage: number;
}

export type NotificationPageData = PageResult<NotificationItem>;
export type NotificationPageResponse = ApiResult<NotificationPageData>;
export type NotificationUnreadCountResponse = ApiResult<NotificationUnreadCount>;
export type NotificationMutationResponse = ApiResult<null>;
