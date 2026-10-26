import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useUserStore } from '@/stores/userStore';
import { updateCurrentUserProfile } from '../api/userApi';
import { useUserSettings } from './useUserSettings';

vi.mock('vue-router', async (importOriginal) => ({
	...(await importOriginal<typeof import('vue-router')>()),
	useRouter: () => ({ replace: vi.fn() }),
}));

vi.mock('../api/userApi', () => ({
	changeCurrentUserPassword: vi.fn(),
	getCurrentUserProfile: vi.fn(),
	updateCurrentUserProfile: vi.fn(),
}));

vi.mock('element-plus', () => ({
	ElMessage: {
		error: vi.fn(),
		success: vi.fn(),
		warning: vi.fn(),
	},
}));

describe('useUserSettings profile changes', () => {
	beforeEach(() => {
		setActivePinia(createPinia());
		vi.clearAllMocks();
		vi.stubGlobal(
			'URL',
			class extends URL {
				static createObjectURL = vi.fn(() => 'blob:avatar-preview');
				static revokeObjectURL = vi.fn();
			},
		);
	});

	it('keeps a cropped avatar as a local draft until profile save succeeds', async () => {
		vi.mocked(updateCurrentUserProfile).mockResolvedValue({
			id: 10001,
			username: 'alice',
			nickname: 'Alice',
			email: 'alice@example.com',
			avatarUrl: 'https://img.example.com/avatar.webp',
			bio: '',
			updatedAt: '2026-08-09T16:00:00+08:00',
		});
		const settings = useUserSettings();
		const { userProfile, avatarDraftUrl, profileChanged, setAvatarDraft } = settings;
		Object.assign(userProfile, {
			id: 10001,
			username: 'alice',
			nickname: 'Alice',
			status: 'ACTIVE',
		});
		const file = new File(['avatar'], 'avatar.webp', { type: 'image/webp' });
		setAvatarDraft(file);

		expect(profileChanged.value).toBe(true);
		expect(userProfile.avatarUrl).toBe('');
		expect(avatarDraftUrl.value).toBe('blob:avatar-preview');
		expect(updateCurrentUserProfile).not.toHaveBeenCalled();

		const form = { validate: vi.fn().mockResolvedValue(undefined), clearValidate: vi.fn() };
		settings.profileForm.nickname = 'Alice';
		settings.setProfileFormRef(form);
		await settings.updateUserSettingsProfile();

		expect(updateCurrentUserProfile).toHaveBeenCalledWith(settings.profileForm, file);
		expect(userProfile.avatarUrl).toBe('https://img.example.com/avatar.webp');
		expect(useUserStore().userInfo?.avatarUrl).toBe('https://img.example.com/avatar.webp');
		expect(avatarDraftUrl.value).toBe('');
	});

	it('accepts a 100-character bio and rejects a longer bio', () => {
		const { profileForm, profileValidated } = useUserSettings();
		profileForm.nickname = 'Alice';
		profileForm.bio = 'a'.repeat(100);

		expect(profileValidated.value).toBe(true);

		profileForm.bio = 'a'.repeat(101);

		expect(profileValidated.value).toBe(false);
	});
});
