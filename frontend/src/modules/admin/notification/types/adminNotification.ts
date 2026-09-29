import type { ApiResult } from '@/types/api';

export type NotificationTargetScope = 'SELECTED_USERS' | 'ALL_USERS';
export type NotificationStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE';

export interface AdminNotificationItem {
	id: number;
	title: string;
	content: string;
	targetScope: NotificationTargetScope;
	userId: number | null;
	status: NotificationStatus;
	createdAt: string;
	publishedAt: string | null;
}

export interface CreateAdminNotificationRequest {
	targetScope: NotificationTargetScope;
	title: string;
	content: string;
	userId?: number;
	status?: 'DRAFT' | 'PUBLISHED';
}

export type UpdateAdminNotificationRequest = Omit<CreateAdminNotificationRequest, 'status'>;

export type AdminNotificationListResponse = ApiResult<AdminNotificationItem[]>;
export type AdminNotificationResponse = ApiResult<AdminNotificationItem>;
