import { logout, refreshToken as refreshTokenApi } from '@/modules/auth/api/authApi';
import type { RefreshTokenData } from '@/modules/auth/types/auth';
import { defineStore } from 'pinia';
import { useUserStore } from './userStore';

export const useAuthStore = defineStore('auth', {
	state: () => ({
		accessToken: '',
		triedRefresh: false, // 无 AT 时会尝试 /auth/refreshToken，这是用于避免重复尝试的标志
	}),

	actions: {
		setAccessToken(token: string) {
			this.accessToken = token;
		},

		clearAuth() {
			this.accessToken = '';
			const userStore = useUserStore();
			userStore.clearUserInfo();
		},

		async refreshToken() {
			const userStore = useUserStore();

			try {
				const data: RefreshTokenData = await refreshTokenApi();
				this.setAccessToken(data.accessToken);
				await userStore.getCurrentUserProfile({
					meta: {
						skipAuthRefresh: true,
						showError: false,
					},
				});
			} catch (err) {
				this.clearAuth();
				throw err;
			}
		},

		async logout() {
			try {
				await logout();
			} finally {
				this.clearAuth();
			}
		},
	},
});
