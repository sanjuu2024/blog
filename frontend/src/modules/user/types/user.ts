import type { ApiResult } from '@/types/api';
import type { PageResult } from '@/types/api';
import type { PublicArticleListItem } from '@/modules/article/types/article';

export const USER_STATUS = {
	ACTIVE: 'ACTIVE',
	DISABLED: 'DISABLED',
} as const;

export type UserStatus = (typeof USER_STATUS)[keyof typeof USER_STATUS];

export const USER_ROLE = {
	ADMIN: 'ADMIN',
	USER: 'USER',
} as const;

export type UserRole = (typeof USER_ROLE)[keyof typeof USER_ROLE];

export interface UserInfo {
	id: number;
	username: string;
	nickname: string;
	email: string;
	emailVerified?: boolean;
	role: UserRole;
	status: UserStatus;
	avatarUrl: string;
	bio: string;
}

export interface CurrentUserInfo extends UserInfo {
	lastLoginAt: string | null;
	createdAt: string;
}

export interface PublicUserProfileData {
	id: number;
	username: string;
	nickname: string;
	avatarUrl: string;
	bio: string;
	deleted?: boolean;
	role: UserRole;
}

export type PublicUserProfileResponse = ApiResult<PublicUserProfileData>;

export type CurrentUserProfileData = CurrentUserInfo;

export type CurrentUserProfileResponse = ApiResult<CurrentUserProfileData>;

export type LikedArticlePageData = PageResult<PublicArticleListItem>;

export type LikedArticlePageResponse = ApiResult<LikedArticlePageData>;

export interface LikedArticleListQuery {
	pageNum?: number;
	pageSize?: number;
}

export interface UpdateCurrentUserProfileRequest {
	nickname?: string;
	bio?: string;
}

export interface UpdatedCurrentUserProfileData {
	id: number;
	username: string;
	nickname: string;
	email: string;
	avatarUrl: string;
	bio: string;
	updatedAt: string;
}

export type UpdatedCurrentUserProfileResponse = ApiResult<UpdatedCurrentUserProfileData>;

export interface UpdatedCurrentUserAvatarData {
	id: number;
	avatarUrl: string;
	updatedAt: string;
}

export type UpdatedCurrentUserAvatarResponse = ApiResult<UpdatedCurrentUserAvatarData>;

export interface ChangeCurrentUserPasswordRequest {
	oldPassword: string;
	newPassword: string;
}

export type ChangeCurrentUserPasswordResponse = ApiResult<null>;

export interface SendEmailChangeCodeRequest {
	email: string;
}

export type SendEmailChangeCodeResponse = ApiResult<null>;

export interface ChangeCurrentUserEmailRequest {
	currentPassword: string;
	newEmail: string;
	verificationCode: string;
}

export type ChangeCurrentUserEmailResponse = ApiResult<UpdatedCurrentUserProfileData>;

export interface DeleteCurrentUserRequest {
	password: string;
}

export type DeleteCurrentUserResponse = ApiResult<null>;
