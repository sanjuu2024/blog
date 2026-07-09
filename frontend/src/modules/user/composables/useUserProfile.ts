import type { FormItemRule } from 'element-plus';
import { reactive, ref, watch } from 'vue';
import { getCurrentUserProfile, updateCurrentUserProfile } from '../api/userApi';
import {
	USER_ROLE,
	USER_STATUS,
	type CurrentUserInfo,
	type UpdateCurrentUserProfileRequest,
	type UpdatedCurrentUserProfileData,
} from '../types/user';

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

	// 用户信息更新参数
	const userProfileForm = reactive<UpdateCurrentUserProfileRequest>({
		nickname: '',
		bio: '',
	});

	// 当前用户信息
	const userProfile = reactive<CurrentUserInfo>({ ...initUserProfile });
	const nicknameAvailable = ref(false);
	const bioValid = ref(true);
	const validated = ref(false);

	// 表单校验规则
	const rules = {
		nickname: [
			{
				required: true,
				trigger: 'change',
				validator: (
					_rule: FormItemRule,
					value: string,
					callback: (error?: Error) => void,
				) => {
					nicknameAvailable.value = false;
					const nickname = value?.trim() ?? '';
					if (!nickname.trim()) {
						callback(new Error('昵称不能为空。'));
					} else if (nickname.length > 20) {
						callback(new Error('昵称长度不能超过 20 个字符。'));
					} else {
						nicknameAvailable.value = true;
						callback();
					}
				},
			},
		],
		bio: [
			{
				trigger: 'change',
				validator: (
					_rule: FormItemRule,
					value: string,
					callback: (error?: Error) => void,
				) => {
					bioValid.value = false;
					if (value.length > 500) {
						callback(new Error('个人简介长度不能超过 500 个字符。'));
					} else {
						bioValid.value = true;
						callback();
					}
				},
			},
		],
	};

	watch(
		() => [nicknameAvailable.value, bioValid.value],
		() => {
			validated.value = nicknameAvailable.value && bioValid.value;
		},
	);

	// 编辑资料前设置用户信息更新参数，并同步初始化按钮状态
	const setEditProfileForm = () => {
		userProfileForm.nickname = userProfile.nickname;
		userProfileForm.bio = userProfile.bio;
		nicknameAvailable.value = Boolean(userProfileForm.nickname?.trim());
		bioValid.value = (userProfileForm.bio?.length ?? 0) <= 500;
		validated.value = nicknameAvailable.value && bioValid.value;
	};

	// 发送 获取个人信息 请求
	async function getUserProfile() {
		try {
			const data = await getCurrentUserProfile();
			Object.assign(userProfile, data);
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	// 发送 更新个人信息 请求
	async function updateUserProfile() {
		try {
			const data: UpdatedCurrentUserProfileData =
				await updateCurrentUserProfile(userProfileForm);
			Object.assign(userProfile, data);

			return true;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
			return false;
		}
	}

	return {
		userProfile,
		userProfileForm,
		rules,
		validated,
		getUserProfile,
		updateUserProfile,
		setEditProfileForm,
	};
}
