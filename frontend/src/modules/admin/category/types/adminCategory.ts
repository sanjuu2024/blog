import type { CategoryLevel, CategoryStatus } from '@/modules/category/constants/category';
import type { ApiResult } from '@/types/api';

export interface AdminCategoryListQuery {
	keyword?: string;
	status?: CategoryStatus;
	level?: CategoryLevel;
	parentId?: number;
}

export interface AdminCategoryItem {
	id: number;
	parentId: number | null;
	level: CategoryLevel;
	name: string;
	description: string;
	sortNo: number;
	status: CategoryStatus;
	articleCount: number;
	createdAt: string;
	children: AdminCategoryItem[];
}

export type AdminCategoryListData = AdminCategoryItem[];

export type AdminCategoryListResponse = ApiResult<AdminCategoryListData>;

export interface CategoryUpsertRequest {
	parentId?: number | null;
	level: CategoryLevel;
	name: string;
	description?: string;
	sortNo?: number;
	status?: CategoryStatus;
}

export interface CreatedCategoryData {
	id: number;
	parentId: number | null;
	level: CategoryLevel;
	name: string;
	status: CategoryStatus;
	createdAt: string;
}

export type CreatedCategoryResponse = ApiResult<CreatedCategoryData>;

export interface UpdatedCategoryData {
	id: number;
	parentId: number | null;
	level: CategoryLevel;
	name: string;
	status: CategoryStatus;
	updatedAt: string;
}

export type UpdatedCategoryResponse = ApiResult<UpdatedCategoryData>;

export type DeleteCategoryResponse = ApiResult<null>;
