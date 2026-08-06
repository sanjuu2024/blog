import type { ApiResult, PageResult } from '@/types/api';

export const COMMENT_STATUS = {
	PENDING: 'PENDING',
	APPROVED: 'APPROVED',
	REJECTED: 'REJECTED',
	HIDDEN: 'HIDDEN',
	DELETED: 'DELETED',
} as const;

export type CommentStatus = (typeof COMMENT_STATUS)[keyof typeof COMMENT_STATUS];

export interface CommentAuthor {
	id: number;
	username: string;
	nickname: string;
	avatarUrl: string;
}

export interface PublicCommentItem {
	id: number;
	articleId: number;
	content: string;
	status: CommentStatus;
	moderationReason: string | null;
	author: CommentAuthor;
	replyCount: number;
	hasVisibleReplies: boolean;
	isMine: boolean;
	createdAt: string;
}

export interface CommentReplyItem extends PublicCommentItem {
	parentId: number;
	rootId: number;
	replyToUser: CommentAuthor | null;
}

export interface PublicCommentListQuery {
	pageNum?: number;
	pageSize?: number;
}

export interface CommentReplyListQuery {
	limit?: number;
	cursor?: string | null;
}

export interface CommentReplyPageData {
	records: CommentReplyItem[];
	nextCursor: string | null;
	hasNext: boolean;
}

export interface CreateCommentRequest {
	content: string;
	parentId?: number;
}

export interface CommentMutationData {
	id: number;
	articleId: number;
	parentId: number | null;
	rootId: number | null;
	content: string;
	status: CommentStatus;
	moderationReason: string | null;
	author: CommentAuthor;
	createdAt: string;
}

export interface CommentDeleteData {
	deletedApprovedCount: number;
}

export interface CommentReplyState {
	records: CommentReplyItem[];
	nextCursor: string | null;
	hasNext: boolean;
	loading: boolean;
	loaded: boolean;
	expanded: boolean;
}

export type PublicCommentPageData = PageResult<PublicCommentItem>;

export type PublicCommentPageResponse = ApiResult<PublicCommentPageData>;

export type CommentReplyPageResponse = ApiResult<CommentReplyPageData>;

export type CommentMutationResponse = ApiResult<CommentMutationData>;

export type DeleteCommentResponse = ApiResult<CommentDeleteData>;
