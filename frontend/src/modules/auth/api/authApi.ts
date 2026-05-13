import request from '@/utils/request';
import type {
	LoginData,
	LoginRequest,
	LoginResponse,
	LogoutResponse,
	RefreshTokenData,
	RefreshTokenResponse,
	RegisterRequest,
	RegisterResponse,
} from '../types/auth';

const AUTH_API = {
	register: 'auth/register',
	login: 'auth/login',
	refreshToken: 'auth/refresh',
	logout: 'auth/logout',
} as const;

// 注册接口
export const register = (data: RegisterRequest): Promise<null> => {
	// 🔺request.post<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data)
	return request.post<RegisterResponse, null, RegisterRequest>(AUTH_API.register, data);
};

// 登录接口
export const login = (data: LoginRequest): Promise<LoginData> => {
	return request.post<LoginResponse, LoginData, LoginRequest>(AUTH_API.login, data);
};

// 刷新 Token 接口
// RT 在浏览器自带的 HttpOnly Cookie 中，前端无法直接访问它，后端会从 Cookie 中自动获取并验证 RT
export const refreshToken = (): Promise<RefreshTokenData> => {
	return request.post<RefreshTokenResponse, RefreshTokenData>(AUTH_API.refreshToken, undefined, {
		meta: {
			skipAuthRefresh: true, // 🔺如果该请求失败，不再重复尝试 /refresh，避免 /refresh 递归请求
			showError: false,
		},
	});
};

// 退出登录接口
// RT 在浏览器自带的 HttpOnly Cookie 中，前端无法直接访问它，后端会从 Cookie 中自动获取并验证 RT
export const logout = (): Promise<null> => {
	return request.post<LogoutResponse, null>(AUTH_API.logout);
};
