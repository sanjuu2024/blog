import type { ApiResult, PageResult } from '@/types/api';
import type { AdminTagItem } from '../../tag/types/adminTag';

// 查询的条件参数
export interface AdminArticleFilterForm {
	title?: string;
	categoryId?: number;
	status?: ArticleStatus | 'ALL';
	isTop?: boolean | 'ALL';
}

// 查询的分页参数
export interface AdminArticlePageParams {
	pageNum: number;
	pageSize: number;
}

// 分页元信息
export interface AdminArticlePageMeta {
	total: number;
	totalPages: number;
	hasNext: boolean;
}

// 标签选择元信息
export interface CheckTagItem extends AdminTagItem {
	checked: boolean;
}

export const ARTICLE_STATUS = {
	DRAFT: 'DRAFT',
	PUBLISHED: 'PUBLISHED',
	OFFLINE: 'OFFLINE',
} as const;

export type ArticleStatus = (typeof ARTICLE_STATUS)[keyof typeof ARTICLE_STATUS];

export interface AdminArticleListQuery {
	pageNum?: number;
	pageSize?: number;
	title?: string;
	categoryId?: number;
	status?: ArticleStatus;
	isTop?: boolean;
}

export interface AdminArticleCategoryParent {
	id: number;
	name: string;
}

export interface AdminArticleCategory {
	id: number;
	name: string;
	level: number;
	parent: AdminArticleCategoryParent;
}

export interface AdminArticleListItem {
	id: number;
	title: string;
	summary: string;
	status: ArticleStatus;
	isTop: boolean;
	coverUrl: string;
	publishedAt: string | null;
	updatedAt: string;
	category: AdminArticleCategory;
}

export type AdminArticlePageData = PageResult<AdminArticleListItem>;

export type AdminArticlePageResponse = ApiResult<AdminArticlePageData>;

export interface AdminArticleDetailData {
	id: number;
	title: string;
	summary: string;
	contentMd: string;
	contentHtml: string;
	contentText: string;
	coverUrl: string;
	status: ArticleStatus;
	isTop: boolean;
	categoryId: number;
	tagIds: number[];
	allowComment: boolean;
	publishedAt: string | null;
	createdAt: string;
	updatedAt: string;
}

export type AdminArticleDetailResponse = ApiResult<AdminArticleDetailData>;

export interface ArticleUpsertRequest {
	title: string;
	summary?: string;
	contentMd: string;
	categoryId: number;
	tagIds?: number[];
	coverUrl?: string;
	isTop?: boolean;
	status: ArticleStatus;
	allowComment?: boolean;
}

export interface CreatedArticleData {
	id: number;
	status: ArticleStatus;
	publishedAt: string | null;
	createdAt: string;
}

export type CreatedArticleResponse = ApiResult<CreatedArticleData>;

export interface UpdatedArticleData {
	id: number;
	status: ArticleStatus;
	publishedAt: string | null;
	updatedAt: string;
}

export type UpdatedArticleResponse = ApiResult<UpdatedArticleData>;

export interface UpdateArticleStatusRequest {
	status: ArticleStatus;
}

export interface UpdatedArticleStatusData {
	id: number;
	status: ArticleStatus;
	updatedAt: string;
}

export type UpdatedArticleStatusResponse = ApiResult<UpdatedArticleStatusData>;

export type DeleteArticleResponse = ApiResult<null>;
