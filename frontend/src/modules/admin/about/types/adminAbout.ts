import type { ApiResult } from '@/types/api';

export interface AboutPageEditor {
	id: number;
	username: string;
}

export interface AdminAboutPageData {
	exists: boolean;
	contentMd: string;
	contentHtml: string;
	contentText: string;
	updatedBy: AboutPageEditor | null;
	updatedAt: string | null;
}

export type AdminAboutPageResponse = ApiResult<AdminAboutPageData>;

export interface UpdateAboutPageRequest {
	contentMd: string;
}

export type UpdateAboutPageResponse = ApiResult<AdminAboutPageData>;
