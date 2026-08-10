import type { ApiResult } from '@/types/api';

export const ADMIN_IMAGE_UPLOAD_SCENE = {
	ARTICLE_COVER: 'ARTICLE_COVER',
	ARTICLE_CONTENT: 'ARTICLE_CONTENT',
	PROJECT_COVER: 'PROJECT_COVER',
} as const;

export type AdminImageUploadScene =
	(typeof ADMIN_IMAGE_UPLOAD_SCENE)[keyof typeof ADMIN_IMAGE_UPLOAD_SCENE];

export interface UploadedImageData {
	url: string;
	originalName: string;
	contentType: string;
	size: number;
}

export type UploadedImageResponse = ApiResult<UploadedImageData>;
