import { reactive } from 'vue';
import { getCurrentUserProfile } from '../api/userApi';
import { USER_ROLE, USER_STATUS, type CurrentUserInfo } from '../types/user';

export function useUserProfile() {
	// 用户信息默认值
	const initUserProfile: CurrentUserInfo = {
		id: 0,
		username: '',
		nickname: '',
		email: '',
		role: USER_ROLE.USER,
		status: USER_STATUS.DISABLED,
		avatarUrl: '',
		bio: '',
		lastLoginAt: null,
		createdAt: '',
	};

	// 当前用户信息
	const userProfile = reactive<CurrentUserInfo>({ ...initUserProfile });

	// 发送 获取个人信息 请求
	async function getUserProfile() {
		try {
			const data = await getCurrentUserProfile();
			Object.assign(userProfile, data);
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	return {
		userProfile,
		getUserProfile,
	};
}
