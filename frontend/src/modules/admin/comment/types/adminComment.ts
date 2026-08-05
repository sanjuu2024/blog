import type { ApiResult, PageResult } from '@/types/api';

export const COMMENT_STATUS = {
	PENDING: 'PENDING',
	APPROVED: 'APPROVED',
	REJECTED: 'REJECTED',
	HIDDEN: 'HIDDEN',
	DELETED: 'DELETED',
} as const;

export type CommentStatus = (typeof COMMENT_STATUS)[keyof typeof COMMENT_STATUS];

export const COMMENT_TYPE = {
	TOP_LEVEL: 'TOP_LEVEL',
	REPLY: 'REPLY',
} as const;

export type CommentType = (typeof COMMENT_TYPE)[keyof typeof COMMENT_TYPE];

export const COMMENT_MODERATION_ACTION = {
	APPROVE: 'APPROVE',
	REJECT: 'REJECT',
	HIDE: 'HIDE',
	DELETE: 'DELETE',
} as const;

export type CommentModerationAction =
	(typeof COMMENT_MODERATION_ACTION)[keyof typeof COMMENT_MODERATION_ACTION];

// 后台评论筛选表单
export interface AdminCommentFilterForm {
	articleId?: number;
	userId?: number;
	status?: CommentStatus | '';
	type?: CommentType | '';
	createdAtRange?: [string, string] | [];
}

// 查询的分页参数
export interface AdminCommentPageParams {
	pageNum: number;
	pageSize: number;
}

// 分页元信息
export interface AdminCommentPageMeta {
	total: number;
	totalPages: number;
	hasNext: boolean;
}

// GET /api/v1/admin/comments 的 Query 参数
export interface AdminCommentListQuery {
	pageNum?: number;
	pageSize?: number;
	articleId?: number;
	userId?: number;
	status?: CommentStatus;
	type?: CommentType;
	createdAtFrom?: string;
	createdAtTo?: string;
}

export interface CommentAuthor {
	id: number;
	username: string;
	nickname: string;
	avatarUrl: string;
}

export interface AdminCommentArticleRef {
	id: number;
	title: string;
}

export interface AdminCommentItem {
	id: number;
	articleId: number;
	parentId: number | null;
	rootId: number | null;
	content: string;
	status: CommentStatus;
	moderationReason: string | null;
	author: CommentAuthor | null;
	reviewedBy: number | null;
	reviewedAt: string | null;
	deletedBy: number | null;
	deletedAt: string | null;
	createdAt: string;
	article: AdminCommentArticleRef | null;
	type: CommentType;
}

export type AdminCommentPageData = PageResult<AdminCommentItem>;

export type AdminCommentPageResponse = ApiResult<AdminCommentPageData>;

export interface CommentModerationRequest {
	action: CommentModerationAction;
	reason?: string;
}

export type CommentMutationData = Omit<AdminCommentItem, 'article' | 'type'>;

export type CommentMutationResponse = ApiResult<CommentMutationData>;
