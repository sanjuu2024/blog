import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { logout, refreshToken } from '@/modules/auth/api/authApi';
import { getCurrentUserProfile } from '@/modules/user/api/userApi';
import { useAuthStore } from './authStore';
import { useUserStore } from './userStore';
import type { CurrentUserInfo } from '@/modules/user/types/user';

vi.mock('@/modules/auth/api/authApi', () => ({
	logout: vi.fn(),
	refreshToken: vi.fn(),
}));

vi.mock('@/modules/user/api/userApi', () => ({
	getCurrentUserProfile: vi.fn(),
}));

const userInfo: CurrentUserInfo = {
	id: 10001,
	username: 'sanjuu',
	nickname: 'Sanjuu',
	email: 'sanjuu@example.com',
	role: 'USER',
	status: 'ACTIVE',
	avatarUrl: '',
	bio: '',
	lastLoginAt: null,
	createdAt: '2026-05-01T10:00:00+08:00',
};

describe('authStore', () => {
	beforeEach(() => {
		setActivePinia(createPinia());
		vi.clearAllMocks();
	});

	it('restores access token and current user from refresh session', async () => {
		vi.mocked(refreshToken).mockResolvedValue({
			accessToken: 'new-access-token',
			accessTokenExpiresAt: '2026-05-01T10:15:00+08:00',
			refreshTokenExpiresAt: '2026-05-08T10:00:00+08:00',
			tokenType: 'Bearer',
		});
		vi.mocked(getCurrentUserProfile).mockResolvedValue(userInfo);

		const authStore = useAuthStore();
		const userStore = useUserStore();
		await authStore.refreshToken();

		expect(authStore.accessToken).toBe('new-access-token');
		expect(userStore.userInfo).toEqual(userInfo);
		expect(getCurrentUserProfile).toHaveBeenCalledWith({
			meta: {
				skipAuthRefresh: true,
				showError: false,
			},
		});
	});

	it('clears local auth when refresh fails', async () => {
		vi.mocked(refreshToken).mockRejectedValue(new Error('refresh failed'));
		const authStore = useAuthStore();
		const userStore = useUserStore();
		authStore.accessToken = 'expired-token';
		userStore.userInfo = userInfo;

		await expect(authStore.refreshToken()).rejects.toThrow('refresh failed');

		expect(authStore.accessToken).toBe('');
		expect(userStore.userInfo).toBeNull();
	});

	it('clears local auth even when logout request fails', async () => {
		vi.mocked(logout).mockRejectedValue(new Error('network failed'));
		const authStore = useAuthStore();
		const userStore = useUserStore();
		authStore.accessToken = 'access-token';
		userStore.userInfo = userInfo;

		await expect(authStore.logout()).rejects.toThrow('network failed');

		expect(authStore.accessToken).toBe('');
		expect(userStore.userInfo).toBeNull();
	});

	it('shares one refresh request between concurrent initializations', async () => {
		vi.mocked(refreshToken).mockResolvedValue({
			accessToken: 'restored-token',
			accessTokenExpiresAt: '2026-05-01T10:15:00+08:00',
			refreshTokenExpiresAt: '2026-05-08T10:00:00+08:00',
			tokenType: 'Bearer',
		});
		vi.mocked(getCurrentUserProfile).mockResolvedValue(userInfo);
		const authStore = useAuthStore();

		await Promise.all([authStore.initializeSession(), authStore.initializeSession()]);

		expect(refreshToken).toHaveBeenCalledTimes(1);
		expect(authStore.authInitialized).toBe(true);
		expect(authStore.accessToken).toBe('restored-token');
	});
});
