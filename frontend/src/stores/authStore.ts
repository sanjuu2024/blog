import { logout, refreshToken as refreshTokenApi } from '@/modules/auth/api/authApi';
import type { RefreshTokenData } from '@/modules/auth/types/auth';
import { defineStore } from 'pinia';
import { useUserStore } from './userStore';

// 维护一个共享初始化 Promise
let initializeSessionPromise: Promise<void> | null = null;

export const useAuthStore = defineStore('auth', {
	state: () => ({
		accessToken: '',
		authInitialized: false, // 表示应用是否已经完成登录态初始化（无 AT 时会尝试 /auth/refresh，这是用于避免重复尝试的标志）
	}),

	getters: {
		isLogin: (state) => Boolean(state.accessToken),
	},

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

		async initializeSession(): Promise<void> {
			if (this.authInitialized) {
				return;
			}

			if (!initializeSessionPromise) {
				initializeSessionPromise = this.refreshToken()
					.catch(() => {
						this.clearAuth();
					})
					.finally(() => {
						this.authInitialized = true;
						initializeSessionPromise = null;
					});
			}

			await initializeSessionPromise;
		},
	},
});
