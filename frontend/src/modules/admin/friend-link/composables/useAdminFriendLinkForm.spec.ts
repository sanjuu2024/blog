import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createAdminFriendLink } from '../api/adminFriendLinkApi';
import { FRIEND_LINK_SECTION, FRIEND_LINK_STATUS } from '../types/adminFriendLink';
import { useAdminFriendLinkForm } from './useAdminFriendLinkForm';

vi.mock('../api/adminFriendLinkApi', () => ({
	createAdminFriendLink: vi.fn(),
	updateAdminFriendLink: vi.fn(),
}));

describe('useAdminFriendLinkForm', () => {
	beforeEach(() => vi.clearAllMocks());

	it('starts new links with the first fixed section, enabled status, and sort 100', () => {
		const form = useAdminFriendLinkForm();
		form.openCreateDrawer();
		expect(form.form.section).toBe(FRIEND_LINK_SECTION.FRIENDS);
		expect(form.form.status).toBe(FRIEND_LINK_STATUS.ENABLED);
		expect(form.form.sortNo).toBe(100);
		expect(form.drawerVisible.value).toBe(true);
	});

	it('loads the complete item when editing', () => {
		const form = useAdminFriendLinkForm();
		form.openEditDrawer({
			id: 110001,
			name: 'Example',
			url: 'https://example.com',
			logoUrl: '',
			description: 'site',
			section: FRIEND_LINK_SECTION.EXPERTS,
			sortNo: 250,
			status: FRIEND_LINK_STATUS.DISABLED,
			createdAt: '',
			updatedAt: '',
		});
		expect(form.form.section).toBe(FRIEND_LINK_SECTION.EXPERTS);
		expect(form.form.sortNo).toBe(250);
		expect(form.form.status).toBe(FRIEND_LINK_STATUS.DISABLED);
	});

	it('validates before submitting and sends trimmed values', async () => {
		const form = useAdminFriendLinkForm();
		const validate = vi.fn().mockResolvedValue(true);
		form.setFormRef({ validate });
		form.openCreateDrawer();
		Object.assign(form.form, {
			name: ' Example ',
			url: ' https://example.com ',
			description: ' Site ',
		});
		vi.mocked(createAdminFriendLink).mockResolvedValue({} as never);

		await expect(form.submitForm()).resolves.toBe(true);
		expect(createAdminFriendLink).toHaveBeenCalledWith(
			expect.objectContaining({
				name: 'Example',
				url: 'https://example.com',
				description: 'Site',
				sortNo: 100,
			}),
		);
	});
});
