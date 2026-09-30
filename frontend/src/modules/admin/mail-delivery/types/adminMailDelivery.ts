import type { ApiResult, PageResult } from '@/types/api';

export type MailType = 'COMMENT_REPLY' | 'MESSAGE_REPLY';
export type MailDeliveryStatus = 'PENDING' | 'SENT' | 'FAILED';

export interface AdminMailDeliveryItem {
	id: number;
	mailType: MailType;
	sourceId: number;
	replyId: number;
	recipientMasked: string;
	status: MailDeliveryStatus;
	attemptCount: number;
	lastErrorType: string | null;
	lastErrorMessage: string | null;
	sentAt: string | null;
	lastAttemptAt: string | null;
	createdAt: string;
}

export interface AdminMailDeliveryListQuery {
	pageNum?: number;
	pageSize?: number;
	mailType?: MailType;
	status?: MailDeliveryStatus;
	createdAtFrom?: string;
	createdAtTo?: string;
}

export type AdminMailDeliveryPageData = PageResult<AdminMailDeliveryItem>;
export type AdminMailDeliveryPageResponse = ApiResult<AdminMailDeliveryPageData>;
export type AdminMailDeliveryResponse = ApiResult<AdminMailDeliveryItem>;
export type AdminMailDeliveryNoDataResponse = ApiResult<null>;
