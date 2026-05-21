import type { ApiResult } from '@/types/api';

export const TAG_STATUS = {
	ENABLED: 'ENABLED',
	DISABLED: 'DISABLED',
} as const;

export type TagStatus = (typeof TAG_STATUS)[keyof typeof TAG_STATUS];

export interface AdminTagListQuery {
	keyword?: string;
	status?: TagStatus;
}

export interface AdminTagItem {
	id: number;
	name: string;
	description: string;
	status: TagStatus;
	articleCount: number;
	createdAt: string;
}

export type AdminTagListData = AdminTagItem[];

export type AdminTagListResponse = ApiResult<AdminTagListData>;

export interface TagUpsertRequest {
	name: string;
	description?: string;
	status?: TagStatus;
}

export interface CreatedTagData {
	id: number;
	name: string;
	status: TagStatus;
	createdAt: string;
}

export type CreatedTagResponse = ApiResult<CreatedTagData>;

export interface UpdatedTagData {
	id: number;
	name: string;
	status: TagStatus;
	updatedAt: string;
}

export type UpdatedTagResponse = ApiResult<UpdatedTagData>;

export type DeleteTagResponse = ApiResult<null>;
