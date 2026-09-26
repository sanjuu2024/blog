import type { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiCode } from '@/constants/apiCode';

const axiosState = vi.hoisted(() => {
	const instance = Object.assign(vi.fn(), {
		interceptors: {
			request: { use: vi.fn() },
			response: { use: vi.fn() },
		},
	});

	return {
		instance,
		isCancel: vi.fn(() => false),
		requestFulfilled: undefined as
			| ((config: InternalAxiosRequestConfig) => InternalAxiosRequestConfig)
			| undefined,
		responseFulfilled: undefined as ((response: { data: unknown }) => unknown) | undefined,
		responseRejected: undefined as ((error: AxiosError) => Promise<unknown>) | undefined,
	};
});

const authStore = vi.hoisted(() => ({
	accessToken: '',
	refreshToken: vi.fn<() => Promise<void>>(),
	clearAuth: vi.fn(),
}));

const router = vi.hoisted(() => ({
	currentRoute: { value: { path: '/admin/articles', fullPath: '/admin/articles' } },
	replace: vi.fn(),
}));

const messageError = vi.hoisted(() => vi.fn());

vi.mock('axios', () => ({
	default: {
		create: vi.fn(() => axiosState.instance),
		isCancel: axiosState.isCancel,
	},
}));

vi.mock('@/stores/authStore', () => ({
	useAuthStore: () => authStore,
}));

vi.mock('@/router', () => ({
	default: router,
}));

vi.mock('element-plus', () => ({
	ElMessage: {
		error: messageError,
	},
}));

await import('./request');

axiosState.requestFulfilled = axiosState.instance.interceptors.request.use.mock.calls[0][0];
axiosState.responseFulfilled = axiosState.instance.interceptors.response.use.mock.calls[0][0];
axiosState.responseRejected = axiosState.instance.interceptors.response.use.mock.calls[0][1];

function requestConfig(overrides: Record<string, unknown> = {}) {
	return {
		headers: {},
		url: '/protected',
		...overrides,
	} as InternalAxiosRequestConfig;
}

function responseError(
	status: number,
	code: number,
	config: InternalAxiosRequestConfig = requestConfig(),
) {
	return {
		config,
		response: {
			status,
			data: { code, message: '请求错误', data: null },
		},
	} as AxiosError;
}

describe('request interceptors', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		authStore.accessToken = '';
		authStore.refreshToken.mockResolvedValue(undefined);
		axiosState.instance.mockResolvedValue('retried-response');
		axiosState.isCancel.mockReturnValue(false);
		router.currentRoute.value = { path: '/admin/articles', fullPath: '/admin/articles' };
		router.replace.mockResolvedValue(undefined);
	});

	it('injects the current access token into protected requests', () => {
		authStore.accessToken = 'access-token';
		const config = requestConfig();

		const result = axiosState.requestFulfilled?.(config);

		expect(result?.headers.Authorization).toBe('Bearer access-token');
	});

	it('unwraps the business data from successful responses', () => {
		const result = axiosState.responseFulfilled?.({
			data: { code: 0, message: '成功', data: { id: 1 } },
		});

		expect(result).toEqual({ id: 1 });
	});

	it('shares one token refresh across concurrent expired requests and retries both', async () => {
		let resolveRefresh!: () => void;
		authStore.refreshToken.mockReturnValue(
			new Promise<void>((resolve) => {
				resolveRefresh = resolve;
			}),
		);
		const firstConfig = requestConfig();
		const secondConfig = requestConfig();

		const first = axiosState.responseRejected?.(
			responseError(401, ApiCode.ACCESS_TOKEN_EXPIRED, firstConfig),
		);
		const second = axiosState.responseRejected?.(
			responseError(401, ApiCode.ACCESS_TOKEN_INVALID, secondConfig),
		);
		expect(authStore.refreshToken).toHaveBeenCalledTimes(1);

		resolveRefresh();
		await expect(first).resolves.toBe('retried-response');
		await expect(second).resolves.toBe('retried-response');
		expect(axiosState.instance).toHaveBeenCalledTimes(2);
		expect(firstConfig._retry).toBe(true);
		expect(secondConfig._retry).toBe(true);
	});

	it('clears auth and redirects when refresh fails', async () => {
		authStore.refreshToken.mockRejectedValue(new Error('refresh failed'));
		const error = responseError(401, ApiCode.ACCESS_TOKEN_EXPIRED);

		await expect(axiosState.responseRejected?.(error)).rejects.toBe(error);

		expect(messageError).toHaveBeenCalledWith('登录状态恢复失败，正在跳转到登录页...');
		expect(authStore.clearAuth).toHaveBeenCalled();
		expect(router.replace).toHaveBeenCalledWith({
			path: '/auth/login',
			query: { redirect: '/admin/articles' },
		});
	});

	it('handles concurrent refresh failures only once', async () => {
		let rejectRefresh!: (reason?: unknown) => void;
		let resolveNavigation!: () => void;
		authStore.refreshToken.mockReturnValue(
			new Promise<void>((_resolve, reject) => {
				rejectRefresh = reject;
			}),
		);
		router.replace.mockReturnValue(
			new Promise<void>((resolve) => {
				resolveNavigation = resolve;
			}),
		);

		const firstError = responseError(401, ApiCode.ACCESS_TOKEN_EXPIRED);
		const secondError = responseError(401, ApiCode.ACCESS_TOKEN_INVALID);
		const first = axiosState.responseRejected?.(firstError);
		const second = axiosState.responseRejected?.(secondError);

		rejectRefresh(new Error('refresh failed'));
		await vi.waitFor(() => {
			expect(router.replace).toHaveBeenCalledTimes(1);
		});
		expect(messageError).toHaveBeenCalledTimes(1);
		expect(authStore.clearAuth).toHaveBeenCalledTimes(1);

		resolveNavigation();
		await expect(first).rejects.toBe(firstError);
		await expect(second).rejects.toBe(secondError);
	});

	it('clears auth silently without redirecting when already on the login page', async () => {
		authStore.refreshToken.mockRejectedValue(new Error('refresh failed'));
		router.currentRoute.value = { path: '/auth/login', fullPath: '/auth/login' };
		const error = responseError(401, ApiCode.ACCESS_TOKEN_EXPIRED);

		await expect(axiosState.responseRejected?.(error)).rejects.toBe(error);

		expect(authStore.clearAuth).toHaveBeenCalledTimes(1);
		expect(messageError).not.toHaveBeenCalled();
		expect(router.replace).not.toHaveBeenCalled();
	});

	it('does not refresh a retried request again', async () => {
		const error = responseError(
			401,
			ApiCode.ACCESS_TOKEN_INVALID,
			requestConfig({ _retry: true }),
		);

		await expect(axiosState.responseRejected?.(error)).rejects.toBe(error);

		expect(authStore.refreshToken).not.toHaveBeenCalled();
		expect(messageError).toHaveBeenCalledWith('登录状态恢复失败，正在跳转到登录页...');
		expect(authStore.clearAuth).toHaveBeenCalled();
		expect(router.replace).toHaveBeenCalledWith({
			path: '/auth/login',
			query: { redirect: '/admin/articles' },
		});
	});

	it('clears auth when the backend reports that the user is disabled', async () => {
		const error = responseError(403, ApiCode.USER_DISABLED);

		await expect(axiosState.responseRejected?.(error)).rejects.toBe(error);

		expect(authStore.clearAuth).toHaveBeenCalled();
		expect(router.replace).toHaveBeenCalled();
		expect(messageError).toHaveBeenCalledWith('请求错误');
	});

	it('leaves privacy policy version conflicts to the registration page', async () => {
		const error = responseError(409, ApiCode.PRIVACY_POLICY_VERSION_MISMATCH);

		await expect(axiosState.responseRejected?.(error)).rejects.toBe(error);

		expect(messageError).not.toHaveBeenCalled();
	});

	it('does not show an error for canceled requests', async () => {
		axiosState.isCancel.mockReturnValue(true);
		const error = { code: 'ERR_CANCELED' } as AxiosError;

		await expect(axiosState.responseRejected?.(error)).rejects.toBe(error);

		expect(messageError).not.toHaveBeenCalled();
	});
});
