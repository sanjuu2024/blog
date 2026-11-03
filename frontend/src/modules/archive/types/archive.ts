import type { PageResult } from '@/types/api';
import type { PublicArticleCategory } from '@/modules/article/types/article';

export type ArchiveMode = 'time' | 'category' | 'tag';

export interface ArchiveYear {
	year: number;
	articleCount: number;
}

export interface ArchiveArticle {
	id: number;
	title: string;
	publishedAt: string;
	category: PublicArticleCategory;
}

export interface ArchiveQuery {
	year?: number;
	categoryId?: number;
	tagId?: number;
}

export type ArchiveArticlePage = PageResult<ArchiveArticle>;
