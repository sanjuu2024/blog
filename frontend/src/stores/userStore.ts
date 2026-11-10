import type { UserInfo } from '@/modules/user/types/user';
import { defineStore } from 'pinia';
import { getCurrentUserProfile } from '@/modules/user/api/userApi';
import type { AxiosRequestConfig } from 'axios';

interface UserState {
	userInfo: UserInfo | null;
}

export const useUserStore = defineStore('user', {
	state: (): UserState => ({
		userInfo: null,
	}),

	actions: {
		setUserInfo(info: UserInfo) {
			this.userInfo = info;
		},

		clearUserInfo() {
			this.userInfo = null;
		},

		async getCurrentUserProfile(config?: AxiosRequestConfig) {
			const userInfo: UserInfo = await getCurrentUserProfile(config);
			this.setUserInfo(userInfo);
		},
	},
});
