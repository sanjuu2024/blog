import request from '@/utils/request';
import type {
	PublicFriendLinkSiteProfile,
	PublicFriendLinkSiteProfileResponse,
	PublicFriendLinkSection,
	PublicFriendLinkSectionsResponse,
} from '../types/friendLink';

export const listPublicFriendLinks = (): Promise<PublicFriendLinkSection[]> => {
	return request.get<PublicFriendLinkSectionsResponse, PublicFriendLinkSection[]>(
		'friend-links',
		{ meta: { showError: false } },
	);
};

export const getPublicFriendLinkSiteProfile = (): Promise<PublicFriendLinkSiteProfile> => {
	return request.get<PublicFriendLinkSiteProfileResponse, PublicFriendLinkSiteProfile>(
		'friend-links/site-profile',
		{ meta: { showError: false } },
	);
};
