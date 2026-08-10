import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/stores/userStore';
import { updateCurrentUserAvatar } from '../api/userApi';
import { useUserSettings } from './useUserSettings';

vi.mock('vue-router', async (importOriginal) => ({
	...(await importOriginal<typeof import('vue-router')>()),
	useRouter: () => ({ replace: vi.fn() }),
}));

vi.mock('../api/userApi', () => ({
	changeCurrentUserPassword: vi.fn(),
	getCurrentUserProfile: vi.fn(),
	updateCurrentUserAvatar: vi.fn(),
	updateCurrentUserProfile: vi.fn(),
}));

vi.mock('element-plus', () => ({
	ElMessage: {
		error: vi.fn(),
		success: vi.fn(),
		warning: vi.fn(),
	},
}));

describe('useUserSettings avatar upload', () => {
	beforeEach(() => {
		setActivePinia(createPinia());
		vi.clearAllMocks();
	});

	it('updates the profile and user store after avatar upload succeeds', async () => {
		vi.mocked(updateCurrentUserAvatar).mockResolvedValue({
			id: 10001,
			avatarUrl: 'https://img.example.com/avatar.jpg',
			updatedAt: '2026-08-09T16:00:00+08:00',
		});
		const file = new File(['avatar'], 'avatar.jpg', { type: 'image/jpeg' });
		const { userProfile, avatarUploading, updateUserSettingsAvatar } = useUserSettings();
		Object.assign(userProfile, {
			id: 10001,
			username: 'alice',
			nickname: 'Alice',
			status: 'ACTIVE',
		});

		const success = await updateUserSettingsAvatar(file);

		expect(success).toBe(true);
		expect(userProfile.avatarUrl).toBe('https://img.example.com/avatar.jpg');
		expect(useUserStore().userInfo?.avatarUrl).toBe('https://img.example.com/avatar.jpg');
		expect(avatarUploading.value).toBe(false);
		expect(ElMessage.success).toHaveBeenCalledWith('头像更新成功');
	});

	it('rejects an oversized avatar before requesting the backend', async () => {
		const file = new File([new Uint8Array(2 * 1024 * 1024 + 1)], 'avatar.jpg', {
			type: 'image/jpeg',
		});
		const { updateUserSettingsAvatar } = useUserSettings();

		const success = await updateUserSettingsAvatar(file);

		expect(success).toBe(false);
		expect(updateCurrentUserAvatar).not.toHaveBeenCalled();
		expect(ElMessage.warning).toHaveBeenCalledWith('图片大小不能超过 2 MB');
	});
});
