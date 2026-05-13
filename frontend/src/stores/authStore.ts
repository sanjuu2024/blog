import { logout, refreshToken } from '@/modules/auth/api/authApi';
import type { RefreshTokenData } from '@/modules/auth/types/auth';
import { defineStore } from 'pinia';

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
		},

		async refreshToken() {
			const data: RefreshTokenData = await refreshToken();
			this.setAccessToken(data.accessToken);
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
