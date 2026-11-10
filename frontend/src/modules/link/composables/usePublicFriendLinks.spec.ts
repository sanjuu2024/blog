import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getPublicFriendLinkSiteProfile, listPublicFriendLinks } from '../api/friendLinkApi';
import type { PublicFriendLinkSection, PublicFriendLinkSiteProfile } from '../types/friendLink';
import { usePublicFriendLinks } from './usePublicFriendLinks';

vi.mock('../api/friendLinkApi', () => ({
	getPublicFriendLinkSiteProfile: vi.fn(),
	listPublicFriendLinks: vi.fn(),
}));

describe('usePublicFriendLinks', () => {
	beforeEach(() => vi.clearAllMocks());

	it('loads the fixed section response and clears the loading state', async () => {
		const sections: PublicFriendLinkSection[] = [
			{ section: 'FRIENDS', name: '友人帐', records: [] },
			{ section: 'EXPERTS', name: '大佬们', records: [] },
			{ section: 'PEERS', name: '同行者', records: [] },
		];
		const siteProfile: PublicFriendLinkSiteProfile = {
			name: 'Example Blog',
			url: 'https://example.com',
			description: 'A blog for testing',
			logoUrl: 'https://example.com/logo.png',
		};
		vi.mocked(listPublicFriendLinks).mockResolvedValue(sections);
		vi.mocked(getPublicFriendLinkSiteProfile).mockResolvedValue(siteProfile);
		const state = usePublicFriendLinks();

		await state.loadFriendLinks();

		expect(state.sections.value).toEqual(sections);
		expect(state.siteProfile.value).toEqual(siteProfile);
		expect(state.loading.value).toBe(false);
		expect(state.loadFailed.value).toBe(false);
	});

	it('exposes a retryable error state when the request fails', async () => {
		vi.mocked(listPublicFriendLinks).mockRejectedValue(new Error('network'));
		vi.mocked(getPublicFriendLinkSiteProfile).mockResolvedValue({
			name: 'Example Blog',
			url: 'https://example.com',
			description: 'A blog for testing',
			logoUrl: 'https://example.com/logo.png',
		});
		const state = usePublicFriendLinks();

		await state.loadFriendLinks();

		expect(state.sections.value).toEqual([]);
		expect(state.loading.value).toBe(false);
		expect(state.loadFailed.value).toBe(true);
	});
});
