import type { ApiResult } from '@/types/api';

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
}

export type PublicUserProfileResponse = ApiResult<PublicUserProfileData>;

export type CurrentUserProfileData = CurrentUserInfo;

export type CurrentUserProfileResponse = ApiResult<CurrentUserProfileData>;

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

export interface ChangeCurrentUserPasswordRequest {
	oldPassword: string;
	newPassword: string;
}

export type ChangeCurrentUserPasswordResponse = ApiResult<null>;
