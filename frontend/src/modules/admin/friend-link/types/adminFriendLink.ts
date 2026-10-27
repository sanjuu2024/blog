import type { ApiResult, PageResult } from '@/types/api';
import type {
	FriendLinkFields,
	FriendLinkSection,
	FriendLinkStatus,
} from '@/modules/link/types/friendLink';

export interface AdminFriendLinkItem extends FriendLinkFields {
	status: FriendLinkStatus;
	createdAt: string;
	updatedAt: string;
}

export interface AdminFriendLinkListQuery {
	pageNum?: number;
	pageSize?: number;
	keyword?: string;
	section?: FriendLinkSection;
	status?: FriendLinkStatus;
}

export interface AdminFriendLinkFilterForm {
	keyword: string;
	section: FriendLinkSection | '';
	status: FriendLinkStatus | '';
}

export type FriendLinkUpsertRequest = Omit<FriendLinkFields, 'id'> & {
	status: FriendLinkStatus;
};

export type AdminFriendLinkPageData = PageResult<AdminFriendLinkItem>;
export type AdminFriendLinkPageResponse = ApiResult<AdminFriendLinkPageData>;
export type FriendLinkMutationResponse = ApiResult<AdminFriendLinkItem>;
export type FriendLinkDeleteResponse = ApiResult<null>;
