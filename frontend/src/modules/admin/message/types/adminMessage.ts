import type { ApiResult, PageResult } from '@/types/api';
import {
	MESSAGE_STATUS,
	type MessageAuthor,
	type MessageMutationData,
	type MessageStatus,
} from '@/modules/message/types/message';

export { MESSAGE_STATUS };
export type { MessageMutationData, MessageStatus };

export const MESSAGE_TYPE = {
	TOP_LEVEL: 'TOP_LEVEL',
	REPLY: 'REPLY',
} as const;

export type MessageType = (typeof MESSAGE_TYPE)[keyof typeof MESSAGE_TYPE];

export const MESSAGE_MODERATION_ACTION = {
	APPROVE: 'APPROVE',
	REJECT: 'REJECT',
	HIDE: 'HIDE',
	DELETE: 'DELETE',
} as const;

export type MessageModerationAction =
	(typeof MESSAGE_MODERATION_ACTION)[keyof typeof MESSAGE_MODERATION_ACTION];

export interface AdminMessageFilterForm {
	messageId?: number;
	userId?: number;
	guestNickname?: string;
	guestEmail?: string;
	content?: string;
	status?: MessageStatus | '';
	type?: MessageType | '';
	createdAtRange?: [string, string] | [];
}

export interface AdminMessagePageParams {
	pageNum: number;
	pageSize: number;
}

export interface AdminMessagePageMeta {
	total: number;
	totalPages: number;
	hasNext: boolean;
}

export interface AdminMessageListQuery {
	pageNum?: number;
	pageSize?: number;
	messageId?: number;
	userId?: number;
	guestNickname?: string;
	guestEmail?: string;
	content?: string;
	status?: MessageStatus;
	type?: MessageType;
	createdAtFrom?: string;
	createdAtTo?: string;
}

export interface AdminMessageItem {
	id: number;
	userId: number | null;
	parentId: number | null;
	nickname: string;
	email: string;
	content: string;
	status: MessageStatus;
	type: MessageType;
	notifyOnReply: boolean;
	moderationReason: string | null;
	reviewedBy: number | null;
	reviewedAt: string | null;
	deletedBy: number | null;
	deletedAt: string | null;
	author: MessageAuthor | null;
	createdAt: string;
}

export type AdminMessagePageData = PageResult<AdminMessageItem>;
export type AdminMessagePageResponse = ApiResult<AdminMessagePageData>;

export interface MessageModerationRequest {
	action: MessageModerationAction;
	reason?: string;
}

export interface CreateMessageReplyRequest {
	content: string;
}

export type MessageMutationResponse = ApiResult<MessageMutationData>;

export interface MessageBatchApprovalRequest {
	messageIds: number[];
}
