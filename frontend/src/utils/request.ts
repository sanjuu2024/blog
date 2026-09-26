import axios from 'axios';
import type { ApiResult } from '@/types/api';
import { ApiCode } from '@/constants/apiCode';
import { useAuthStore } from '@/stores/authStore';
import router from '@/router';

// ES module：同一个模块在同一个 JS 运行环境里只会初始化一次，之后所有 import 拿到的都是同一个模块实例。
// 因此在这个 request 模块里维护一个 refreshPromise 变量，可以让所有请求共享它；
// 防止多个请求同时触发 token 刷新，会导致后端 refresh token 轮换时互相覆盖。
let refreshPromise: Promise<void> | null = null; // 当前有没有正在进行的 refresh 请求
let sessionFailurePromise: Promise<void> | null = null; // 当前有没有正在处理登录态失效

function refreshSession() {
	const authStore = useAuthStore();

	if (!refreshPromise) {
		refreshPromise = authStore.refreshToken().finally(() => {
			refreshPromise = null;
		});
	}

	return refreshPromise;
}

function handleSessionFailure() {
	if (!sessionFailurePromise) {
		sessionFailurePromise = (async () => {
			useAuthStore().clearAuth();

			// 已经位于登录页时无需提示或重复跳转。
			if (router.currentRoute.value.path === '/auth/login') return;

			ElMessage.error('登录状态恢复失败，正在跳转到登录页...');
			await router.replace({
				path: '/auth/login',
				query: { redirect: router.currentRoute.value.fullPath },
			});
		})().finally(() => {
			sessionFailurePromise = null;
		});
	}

	return sessionFailurePromise;
}

// 1. 创建 axios 实例
const request = axios.create({
	baseURL: import.meta.env.VITE_API_BASE_URL, // 请求接口基础 url
	timeout: 5000, // 请求超时时间
	paramsSerializer: {
		indexes: null, // 当 query 参数有数组时，格式会 e.g. tagIds=1&tagIds=2，而不是 tagIds[]=1&tagIds[]=2（虽然两种格式后端目前写法都能正常接收处理）
	},
});

// 2. 为实例添加请求拦截器
request.interceptors.request.use((config) => {
	const authStore = useAuthStore();
	if (authStore.accessToken) {
		config.headers.Authorization = `Bearer ${authStore.accessToken}`;
	}
	return config;
});

// 3. 为实例添加响应拦截器
request.interceptors.response.use(
	(res) => {
		return res.data.data;
	},
	async (err) => {
		// 🍉忽略取消了的请求的错误提示
		if (axios.isCancel(err) || err.code === 'ERR_CANCELED') {
			return Promise.reject(err);
		}

		const showError = err.config?.meta?.showError !== false; // 只要 showError 不是明确的 false，就当作 true
		let errMsg = err.response?.data?.message || '请求失败，请稍后重试';
		const code = (err.response?.data as ApiResult | undefined)?.code;

		if (err.response) {
			// 有响应
			const { status } = err.response;

			switch (status) {
				case 401: {
					switch (code) {
						case ApiCode.ACCESS_TOKEN_EXPIRED:
						case ApiCode.ACCESS_TOKEN_INVALID: {
							// Axios 会把发起这次失败请求时用的配置挂在 err.config 上
							const originalRequest = err.config;

							// 极端异常情况；或原请求设置了“本次请求失败后不再尝试刷新 token”
							if (!originalRequest || originalRequest.meta?.skipAuthRefresh) {
								return Promise.reject(err);
							}

							// 已尝试过原请求失败之后刷新 token；虽然这次原请求还是失败，但不再尝试刷新 token 了，避免死循环
							if (originalRequest._retry) {
								await handleSessionFailure();

								return Promise.reject(err);
							}

							originalRequest._retry = true; // 已尝试刷新 token

							try {
								await refreshSession(); // 等待刷新 token 的请求完成 / 申请刷新 token
								return request(originalRequest); // 🔺刷新 token 后重试原请求
							} catch {
								await handleSessionFailure();

								return Promise.reject(err);
							}
						}
					}
					break;
				}
				case 403: {
					if (code === ApiCode.USER_DISABLED) {
						const authStore = useAuthStore();
						authStore.clearAuth();
						router.replace({
							path: '/auth/login',
							query: { redirect: router.currentRoute.value.fullPath },
						});
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

		if (showError && code !== ApiCode.PRIVACY_POLICY_VERSION_MISMATCH) {
			ElMessage.error(errMsg);
		}

		return Promise.reject(err);
	},
);

// 4. 对外暴露 axios 实例
export default request;
