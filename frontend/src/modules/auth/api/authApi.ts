import request from '@/utils/request';
import type { LoginRequest, RegisterRequest } from '../types/auth';

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
// RT 在浏览器自带的 HttpOnly Cookie 中，前端无法直接访问它，后端会从 Cookie 中自动获取并验证 RT
export const refreshToken = () => {
	return request.post(AUTH_API.refreshToken);
};

// 退出登录接口
// RT 在浏览器自带的 HttpOnly Cookie 中，前端无法直接访问它，后端会从 Cookie 中自动获取并验证 RT
export const logout = () => {
	return request.post(AUTH_API.logout);
};
