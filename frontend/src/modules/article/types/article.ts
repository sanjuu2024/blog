import type { CATEGORY_LEVEL } from '@/modules/category/constants/category';
import type { ApiResult, PageResult } from '@/types/api';

// 查询的条件参数
export interface ArticleFilterForm {
	keyword: string;
	categoryId?: number;
	tagIds?: number[];
}

// 查询的分页参数
export interface ArticlePageParams {
	pageNum: number;
	pageSize: number;
	hasNext: boolean;
}

export type PublicArticleSort = 'DEFAULT' | 'LATEST';

// GET /api/v1/articles 的 Query 参数
export interface PublicArticleListQuery {
	pageNum?: number;
	pageSize?: number;
	keyword?: string;
	categoryId?: number;
	tagIds?: number[];
	isTop?: boolean;
	sort?: PublicArticleSort;
}

export interface PublicArticleCategoryParent {
	id: number;
	name: string;
}

export interface PublicArticleCategory {
	id: number;
	name: string;
	level: typeof CATEGORY_LEVEL.SECOND;
	parent: PublicArticleCategoryParent;
}

export interface PublicArticleTag {
	id: number;
	name: string;
}

export interface PublicArticleAuthor {
	id: number;
	username: string;
	nickname: string;
	avatarUrl: string;
	bio: string;
}

export interface PublicArticleListItem {
	id: number;
	title: string;
	summary: string;
	highlightedTitle?: string;
	searchSnippet?: string;
	coverUrl: string;
	isTop: boolean;
	publishedAt: string;
	viewCount: number;
	category: PublicArticleCategory;
	tags: PublicArticleTag[];
}

export type PublicArticlePageData = PageResult<PublicArticleListItem>;

export type PublicArticlePageResponse = ApiResult<PublicArticlePageData>;

export interface PublicArticleDetailData {
	id: number;
	title: string;
	summary: string;
	contentHtml: string;
	coverUrl: string;
	isTop: boolean;
	allowComment: boolean;
	viewCount: number;
	commentCount: number;
	likeCount: number;
	favoriteCount: number;
	publishedAt: string;
	updatedAt: string;
	category: PublicArticleCategory;
	tags: PublicArticleTag[];
	author: PublicArticleAuthor;
}

export type PublicArticleDetailResponse = ApiResult<PublicArticleDetailData>;
