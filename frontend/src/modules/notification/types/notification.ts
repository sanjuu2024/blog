import type { ApiResult, PageResult } from '@/types/api';

export const NOTIFICATION_CATEGORY = {
	ALL: 'ALL',
	REPLY: 'REPLY',
	ADMIN_MESSAGE: 'ADMIN_MESSAGE',
} as const;

export type NotificationCategory =
	(typeof NOTIFICATION_CATEGORY)[keyof typeof NOTIFICATION_CATEGORY];

export const NOTIFICATION_TYPE = {
	COMMENT_REPLY: 'COMMENT_REPLY',
	MESSAGE_REPLY: 'MESSAGE_REPLY',
	ADMIN_MESSAGE: 'ADMIN_MESSAGE',
} as const;

export type NotificationType = (typeof NOTIFICATION_TYPE)[keyof typeof NOTIFICATION_TYPE];

export interface NotificationItem {
	id: number;
	type: NotificationType;
	title: string;
	content: string;
	sourceId: number | null;
	authorName: string | null;
	originalContent: string | null;
	articleId: number | null;
	parentId: number | null;
	likeCount?: number;
	liked?: boolean;
	canInteract?: boolean;
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
