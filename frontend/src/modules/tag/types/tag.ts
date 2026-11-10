import type { ApiResult } from '@/types/api';

export interface PublicTagItem {
	id: number;
	name: string;
	articleCount: number;
}

export type PublicTagListData = PublicTagItem[];

export type PublicTagListResponse = ApiResult<PublicTagListData>;
