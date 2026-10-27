import type { ApiResult } from '@/types/api';

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

export interface FriendLinkFields {
	id: number;
	name: string;
	url: string;
	logoUrl: string;
	description: string;
	section: FriendLinkSection;
	sortNo: number;
}

export type PublicFriendLinkItem = FriendLinkFields;

export interface PublicFriendLinkSection {
	section: FriendLinkSection;
	name: string;
	records: PublicFriendLinkItem[];
}

export interface PublicFriendLinkSiteProfile {
	name: string;
	url: string;
	description: string;
	logoUrl: string;
}

export type PublicFriendLinkSectionsResponse = ApiResult<PublicFriendLinkSection[]>;
export type PublicFriendLinkSiteProfileResponse = ApiResult<PublicFriendLinkSiteProfile>;
