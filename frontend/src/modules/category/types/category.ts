import type { ApiResult } from '@/types/api';
import type { CategoryLevel } from '../constants/category';

export interface PublicCategoryItem {
	id: number;
	parentId: number | null;
	name: string;
	level: CategoryLevel;
	description: string;
	sortNo: number;
	articleCount: number;
	children: PublicCategoryItem[];
}

export type PublicCategoryListData = PublicCategoryItem[];

export type PublicCategoryListResponse = ApiResult<PublicCategoryListData>;
