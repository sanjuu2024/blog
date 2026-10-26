import request from '@/utils/request';
import type { AxiosRequestConfig } from 'axios';
import type {
	ChangeCurrentUserPasswordRequest,
	ChangeCurrentUserPasswordResponse,
	ChangeCurrentUserEmailRequest,
	ChangeCurrentUserEmailResponse,
	DeleteCurrentUserRequest,
	DeleteCurrentUserResponse,
	CurrentUserProfileData,
	CurrentUserProfileResponse,
	PublicUserProfileData,
	PublicUserProfileResponse,
	UpdatedCurrentUserProfileData,
	UpdateCurrentUserProfileRequest,
	UpdatedCurrentUserProfileResponse,
	LikedArticlePageData,
	LikedArticlePageResponse,
	LikedArticleListQuery,
	SendEmailChangeCodeRequest,
	SendEmailChangeCodeResponse,
} from '../types/user';

const USER_API = {
	publicProfile: (userId: number) => `users/${userId}/public-profile`,
	currentProfile: 'users/me',
	likedArticles: 'users/me/liked-articles',
	updateCurrentProfile: 'users/me/profile',
	changeCurrentPassword: 'users/me/password',
	sendEmailChangeCode: 'users/me/email-verification-codes',
	changeCurrentEmail: 'users/me/email',
	deleteCurrentUser: 'users/me',
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

// 获取当前用户点赞过的文章
export const getLikedArticles = (params?: LikedArticleListQuery): Promise<LikedArticlePageData> => {
	return request.get<LikedArticlePageResponse, LikedArticlePageData>(USER_API.likedArticles, {
		params,
	});
};

// 获取用户公开资料接口
export const getPublicUserProfile = (
	userId: number,
	config?: AxiosRequestConfig,
): Promise<PublicUserProfileData> => {
	return request.get<PublicUserProfileResponse, PublicUserProfileData>(
		USER_API.publicProfile(userId),
		config,
	);
};

// 更新个人资料接口；头像草稿随公开资料一起保存
export const updateCurrentUserProfile = (
	data: UpdateCurrentUserProfileRequest,
	avatar?: File,
): Promise<UpdatedCurrentUserProfileData> => {
	const formData = new FormData();
	if (data.nickname !== undefined) formData.append('nickname', data.nickname);
	if (data.bio !== undefined) formData.append('bio', data.bio);
	if (avatar) formData.append('avatar', avatar);

	return request.put<UpdatedCurrentUserProfileResponse, UpdatedCurrentUserProfileData, FormData>(
		USER_API.updateCurrentProfile,
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

// 发送修改邮箱验证码接口
export const sendEmailChangeCode = (data: SendEmailChangeCodeRequest): Promise<null> => {
	// 🔺request.post<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data)
	return request.post<SendEmailChangeCodeResponse, null, SendEmailChangeCodeRequest>(
		USER_API.sendEmailChangeCode,
		data,
		{ timeout: 30000 },
	);
};

// 修改当前用户邮箱接口
export const changeCurrentUserEmail = (
	data: ChangeCurrentUserEmailRequest,
): Promise<UpdatedCurrentUserProfileData> => {
	return request.put<
		ChangeCurrentUserEmailResponse,
		UpdatedCurrentUserProfileData,
		ChangeCurrentUserEmailRequest
	>(USER_API.changeCurrentEmail, data);
};

// 注销当前用户账号接口
export const deleteCurrentUser = (data: DeleteCurrentUserRequest): Promise<null> => {
	return request.delete<DeleteCurrentUserResponse, null, DeleteCurrentUserRequest>(
		USER_API.deleteCurrentUser,
		{ data },
	);
};
