import axios from 'axios';
import { showErrorMessage } from './feedback';
import type { ApiResult } from '@/types/api';
import { ApiCode } from '@/constants/apiCode';

// 1. 创建 axios 实例
const request = axios.create({
	baseURL: import.meta.env.VITE_API_BASE_URL, // 请求接口基础 url
	timeout: 5000, // 请求超时时间
});

// 2. 为实例添加请求拦截器
request.interceptors.request.use((config) => {
	return config;
});

// 3. 为实例添加响应拦截器
request.interceptors.response.use(
	(res) => {
		console.log(res);
		return res.data;
	},
	(err) => {
		console.log(err);

		const showError = err.config?.meta?.showError !== false; // 只要 showError 不是明确的 false，就当作 true
		let errMsg = err.response?.data?.message || '请求失败，请稍后重试';

		if (err.response) {
			// 有响应
			const { status } = err.response;
			const { code } = err.response.data as ApiResult; // 默认 T 是 unknown

			switch (status) {
				case 401: {
					switch (code) {
						case ApiCode.ACCESS_TOKEN_EXPIRED:
							// 发送 /auth/refresh 请求
							break;
						case ApiCode.ACCESS_TOKEN_INVALID:
							// 跳转到登录页
							break;
					}
					break;
				}
				case 403: {
					if (code === ApiCode.USER_DISABLED) {
						// 跳转到登录页
					}
					break;
				}
			}
		} else if (err.request) {
			// 已发出请求，但没有收到响应
			errMsg = '网络异常，请稍后重试';
		} else {
			errMsg = '请求未发出：' + err.message;
		}

		if (showError) showErrorMessage(errMsg); // 在页面上弹出错误信息提示

		return Promise.reject(err);
	},
);

// 4. 对外暴露 axios 实例
export default request;
