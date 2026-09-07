import type { ApiResult, PageResult } from '@/types/api';

export const MESSAGE_STATUS = {
	PENDING: 'PENDING',
	APPROVED: 'APPROVED',
	REJECTED: 'REJECTED',
	HIDDEN: 'HIDDEN',
	DELETED: 'DELETED',
} as const;

export type MessageStatus = (typeof MESSAGE_STATUS)[keyof typeof MESSAGE_STATUS];

export interface MessageAuthor {
	id: number;
	username: string;
	nickname: string;
	avatarUrl: string | null;
}

export interface MessageReply {
	id: number;
	parentId: number;
	content: string;
	status: MessageStatus;
	author: MessageAuthor | null;
	createdAt: string;
}

export interface PublicMessageItem {
	id: number;
	nickname: string;
	content: string;
	status: MessageStatus;
	moderationReason: string | null;
	author: MessageAuthor | null;
	isMine: boolean;
	replies: MessageReply[];
	createdAt: string;
}

export interface MessagePageQuery {
	pageNum?: number;
	pageSize?: number;
}

export interface CreateMessageRequest {
	nickname?: string;
	email?: string;
	content: string;
	notifyOnReply?: boolean;
}

export interface MessageMutationData {
	id: number;
	parentId: number | null;
	nickname: string;
	content: string;
	status: MessageStatus;
	moderationReason: string | null;
	author: MessageAuthor | null;
	isMine: boolean;
	replies: MessageReply[];
	createdAt: string;
}

export type PublicMessagePageData = PageResult<PublicMessageItem>;
export type PublicMessagePageResponse = ApiResult<PublicMessagePageData>;
export type MessageMutationResponse = ApiResult<MessageMutationData>;

export interface MessageUnsubscribeRequest {
	token: string;
}

export type MessageUnsubscribeResponse = ApiResult<null>;
