import type { ApiResult } from '@/types/api';

export type UserRole = 'ADMIN' | 'USER';

export type UserStatus = 'ACTIVE' | 'DISABLED';

export type TokenType = 'Bearer';

export interface RegisterRequest {
	username: string;
	email: string;
	password: string;
}

export type RegisterResponse = ApiResult<null>;

export interface LoginRequest {
	account: string;
	password: string;
}

export interface LoginUserInfo {
	id: number;
	username: string;
	nickname: string;
	email: string;
	role: UserRole;
	status: UserStatus;
	avatarUrl: string;
	bio: string;
}

export interface AuthSessionData {
	accessToken: string;
	accessTokenExpiresAt: string;
	refreshTokenExpiresAt: string;
	tokenType: TokenType;
}

export interface LoginData extends AuthSessionData {
	user: LoginUserInfo;
}

export type LoginResponse = ApiResult<LoginData>;

export type RefreshTokenData = AuthSessionData;

export type RefreshTokenResponse = ApiResult<RefreshTokenData>;

export type LogoutResponse = ApiResult<null>;
