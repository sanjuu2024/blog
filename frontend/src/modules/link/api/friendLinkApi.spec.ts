import { beforeEach, describe, expect, it, vi } from 'vitest';
import request from '@/utils/request';
import { getPublicFriendLinkSiteProfile, listPublicFriendLinks } from './friendLinkApi';

vi.mock('@/utils/request', () => ({
	default: { get: vi.fn() },
}));

describe('public friend-link API contract', () => {
	beforeEach(() => vi.clearAllMocks());

	it('requests the public grouped list without admin query parameters', () => {
		listPublicFriendLinks();

		expect(request.get).toHaveBeenCalledWith('friend-links', {
			meta: { showError: false },
		});
	});

	it('requests the public site profile without authentication', () => {
		getPublicFriendLinkSiteProfile();

		expect(request.get).toHaveBeenCalledWith('friend-links/site-profile', {
			meta: { showError: false },
		});
	});
});
