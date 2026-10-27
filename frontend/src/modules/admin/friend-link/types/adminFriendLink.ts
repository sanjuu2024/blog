import type { ApiResult, PageResult } from '@/types/api';

export const FRIEND_LINK_SECTION = {
	FRIENDS: 'FRIENDS',
	EXPERTS: 'EXPERTS',
	PEERS: 'PEERS',
} as const;

export type FriendLinkSection = (typeof FRIEND_LINK_SECTION)[keyof typeof FRIEND_LINK_SECTION];

export const FRIEND_LINK_STATUS = {
	ENABLED: 'ENABLED',
	DISABLED: 'DISABLED',
} as const;

export type FriendLinkStatus = (typeof FRIEND_LINK_STATUS)[keyof typeof FRIEND_LINK_STATUS];

export const FRIEND_LINK_SECTIONS: { value: FriendLinkSection; label: string }[] = [
	{ value: FRIEND_LINK_SECTION.FRIENDS, label: '友人帐' },
	{ value: FRIEND_LINK_SECTION.EXPERTS, label: '大佬们' },
	{ value: FRIEND_LINK_SECTION.PEERS, label: '同行者' },
];

export interface AdminFriendLinkItem {
	id: number;
	name: string;
	url: string;
	logoUrl: string;
	description: string;
	section: FriendLinkSection;
	sortNo: number;
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

export interface FriendLinkUpsertRequest {
	name: string;
	url: string;
	logoUrl: string;
	description: string;
	section: FriendLinkSection;
	sortNo: number;
	status: FriendLinkStatus;
}

export type AdminFriendLinkPageData = PageResult<AdminFriendLinkItem>;
export type AdminFriendLinkPageResponse = ApiResult<AdminFriendLinkPageData>;
export type FriendLinkMutationResponse = ApiResult<AdminFriendLinkItem>;
export type FriendLinkDeleteResponse = ApiResult<null>;
