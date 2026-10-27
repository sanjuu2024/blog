import { beforeEach, describe, expect, it, vi } from 'vitest';
import request from '@/utils/request';
import {
	createAdminFriendLink,
	deleteAdminFriendLink,
	listAdminFriendLinks,
	updateAdminFriendLink,
} from './adminFriendLinkApi';
import { FRIEND_LINK_SECTION, FRIEND_LINK_STATUS } from '../types/adminFriendLink';

vi.mock('@/utils/request', () => ({
	default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

describe('admin friend-link API contracts', () => {
	beforeEach(() => vi.clearAllMocks());

	it('passes paging and filters as query params', () => {
		const params = {
			pageNum: 2,
			pageSize: 20,
			keyword: 'site',
			section: FRIEND_LINK_SECTION.FRIENDS,
		};
		listAdminFriendLinks(params);
		expect(request.get).toHaveBeenCalledWith('admin/friend-links', { params });
	});

	it('creates and updates a friend link with its configured sort and status', () => {
		const data = {
			name: 'Example',
			url: 'https://example.com',
			logoUrl: '',
			description: '',
			section: FRIEND_LINK_SECTION.PEERS,
			sortNo: 100,
			status: FRIEND_LINK_STATUS.ENABLED,
		};
		createAdminFriendLink(data);
		updateAdminFriendLink(110001, data);
		expect(request.post).toHaveBeenCalledWith('admin/friend-links', data);
		expect(request.put).toHaveBeenCalledWith('admin/friend-links/110001', data);
	});

	it('deletes the selected friend-link resource', () => {
		deleteAdminFriendLink(110001);
		expect(request.delete).toHaveBeenCalledWith('admin/friend-links/110001');
	});
});
