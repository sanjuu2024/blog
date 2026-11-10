import request from '@/utils/request';
import type {
	AdminFriendLinkListQuery,
	AdminFriendLinkPageData,
	AdminFriendLinkPageResponse,
	FriendLinkDeleteResponse,
	FriendLinkMutationResponse,
	FriendLinkUpsertRequest,
} from '../types/adminFriendLink';

const ADMIN_FRIEND_LINK_API = {
	list: 'admin/friend-links',
	create: 'admin/friend-links',
	update: (friendLinkId: number) => 'admin/friend-links/' + friendLinkId,
	delete: (friendLinkId: number) => 'admin/friend-links/' + friendLinkId,
} as const;

type FriendLinkMutationData = AdminFriendLinkPageData['records'][number];

export const listAdminFriendLinks = (
	params: AdminFriendLinkListQuery,
): Promise<AdminFriendLinkPageData> => {
	return request.get<
		AdminFriendLinkPageResponse,
		AdminFriendLinkPageData,
		AdminFriendLinkListQuery
	>(ADMIN_FRIEND_LINK_API.list, { params });
};

export const createAdminFriendLink = (
	data: FriendLinkUpsertRequest,
): Promise<FriendLinkMutationData> => {
	return request.post<
		FriendLinkMutationResponse,
		FriendLinkMutationData,
		FriendLinkUpsertRequest
	>(ADMIN_FRIEND_LINK_API.create, data);
};

export const updateAdminFriendLink = (
	friendLinkId: number,
	data: FriendLinkUpsertRequest,
): Promise<FriendLinkMutationData> => {
	return request.put<FriendLinkMutationResponse, FriendLinkMutationData, FriendLinkUpsertRequest>(
		ADMIN_FRIEND_LINK_API.update(friendLinkId),
		data,
	);
};

export const deleteAdminFriendLink = (friendLinkId: number): Promise<null> => {
	return request.delete<FriendLinkDeleteResponse, null, null>(
		ADMIN_FRIEND_LINK_API.delete(friendLinkId),
	);
};
