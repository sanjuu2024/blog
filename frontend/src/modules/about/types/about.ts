import type { ApiResult } from '@/types/api';

export interface PublicAboutPageData {
	exists: boolean;
	contentHtml: string;
	contentText: string;
	updatedAt: string | null;
}

export type PublicAboutPageResponse = ApiResult<PublicAboutPageData>;
