import type { ApiResult } from '@/types/api';

export interface SeoMetadata {
	title: string;
	description: string;
	canonicalUrl: string;
	imageUrl: string | null;
	type: 'website' | 'article';
	publishedAt: string | null;
	updatedAt: string | null;
}

export type SeoMetadataResponse = ApiResult<SeoMetadata>;
