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

export interface AuthTokenData {
	accessToken: string;
	accessTokenExpiresAt: string;
	refreshToken: string;
	refreshTokenExpiresAt: string;
	tokenType: TokenType;
}

export interface LoginData extends AuthTokenData {
	user: LoginUserInfo;
}

export type LoginResponse = ApiResult<LoginData>;

export interface RefreshTokenRequest {
	refreshToken: string;
}

export type RefreshTokenData = AuthTokenData;

export type RefreshTokenResponse = ApiResult<RefreshTokenData>;

export interface LogoutRequest {
	refreshToken: string;
}

export type LogoutResponse = ApiResult<null>;
