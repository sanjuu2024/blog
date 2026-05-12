import request from '@/utils/request';
import type {
	LoginRequest,
	LogoutRequest,
	RefreshTokenRequest,
	RegisterRequest,
} from '../types/auth';

const AUTH_API = {
	register: 'auth/register',
	login: 'auth/login',
	refreshToken: 'auth/refresh',
	logout: 'auth/logout',
} as const;

// 注册接口
export const register = (data: RegisterRequest) => {
	return request.post(AUTH_API.register, data);
};

// 登录接口
export const login = (data: LoginRequest) => {
	return request.post(AUTH_API.login, data);
};

// 刷新 Token 接口
export const refreshToken = (data: RefreshTokenRequest) => {
	return request.post(AUTH_API.refreshToken, data);
};

// 退出登录接口
export const logout = (data: LogoutRequest) => {
	return request.post(AUTH_API.logout, data);
};
