import request from '@/utils/request';
import type { AxiosRequestConfig } from 'axios';
import type {
	ChangeCurrentUserPasswordRequest,
	ChangeCurrentUserPasswordResponse,
	CurrentUserProfileData,
	CurrentUserProfileResponse,
	PublicUserProfileData,
	PublicUserProfileResponse,
	UpdatedCurrentUserProfileData,
	UpdateCurrentUserProfileRequest,
	UpdatedCurrentUserAvatarData,
	UpdatedCurrentUserAvatarResponse,
	UpdatedCurrentUserProfileResponse,
} from '../types/user';

const USER_API = {
	publicProfile: (userId: number) => `users/${userId}/public-profile`,
	currentProfile: 'users/me',
	updateCurrentProfile: 'users/me/profile',
	updateCurrentAvatar: 'users/me/avatar',
	changeCurrentPassword: 'users/me/password',
} as const;

// 个人中心信息接口
export const getCurrentUserProfile = (
	config?: AxiosRequestConfig,
): Promise<CurrentUserProfileData> => {
	return request.get<CurrentUserProfileResponse, CurrentUserProfileData>(
		USER_API.currentProfile,
		config,
	);
};

// 获取用户公开资料接口
export const getPublicUserProfile = (userId: number): Promise<PublicUserProfileData> => {
	return request.get<PublicUserProfileResponse, PublicUserProfileData>(
		USER_API.publicProfile(userId),
	);
};

// 更新个人资料接口
export const updateCurrentUserProfile = (
	data: UpdateCurrentUserProfileRequest,
): Promise<UpdatedCurrentUserProfileData> => {
	return request.put<
		UpdatedCurrentUserProfileResponse,
		UpdatedCurrentUserProfileData,
		UpdateCurrentUserProfileRequest
	>(USER_API.updateCurrentProfile, data);
};

// 上传并更新当前用户头像
export const updateCurrentUserAvatar = (file: File): Promise<UpdatedCurrentUserAvatarData> => {
	const formData = new FormData();
	formData.append('file', file);

	return request.put<UpdatedCurrentUserAvatarResponse, UpdatedCurrentUserAvatarData, FormData>(
		USER_API.updateCurrentAvatar,
		formData,
		{ timeout: 30000 },
	);
};

// 修改密码接口
export const changeCurrentUserPassword = (
	data: ChangeCurrentUserPasswordRequest,
): Promise<null> => {
	return request.put<ChangeCurrentUserPasswordResponse, null, ChangeCurrentUserPasswordRequest>(
		USER_API.changeCurrentPassword,
		data,
	);
};
