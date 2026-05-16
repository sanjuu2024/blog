import type { ApiResult } from '@/types/api';
import type { UserInfo } from '@/modules/user/types/user';

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

export type LoginUserInfo = UserInfo;

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
