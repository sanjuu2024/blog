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
	currentRoute: { value: { fullPath: '/admin/articles' } },
	replace: vi.fn(),
}));

const showErrorMessage = vi.hoisted(() => vi.fn());

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

vi.mock('./feedback', () => ({
	showErrorMessage,
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

		expect(authStore.clearAuth).toHaveBeenCalled();
		expect(router.replace).toHaveBeenCalledWith({
			path: '/auth/login',
			query: { redirect: '/admin/articles' },
		});
	});

	it('does not refresh a retried request again', async () => {
		const error = responseError(
			401,
			ApiCode.ACCESS_TOKEN_INVALID,
			requestConfig({ _retry: true }),
		);

		await expect(axiosState.responseRejected?.(error)).rejects.toBe(error);

		expect(authStore.refreshToken).not.toHaveBeenCalled();
		expect(authStore.clearAuth).toHaveBeenCalled();
	});

	it('clears auth when the backend reports that the user is disabled', async () => {
		const error = responseError(403, ApiCode.USER_DISABLED);

		await expect(axiosState.responseRejected?.(error)).rejects.toBe(error);

		expect(authStore.clearAuth).toHaveBeenCalled();
		expect(router.replace).toHaveBeenCalled();
		expect(showErrorMessage).toHaveBeenCalledWith('请求错误');
	});

	it('does not show an error for canceled requests', async () => {
		axiosState.isCancel.mockReturnValue(true);
		const error = { code: 'ERR_CANCELED' } as AxiosError;

		await expect(axiosState.responseRejected?.(error)).rejects.toBe(error);

		expect(showErrorMessage).not.toHaveBeenCalled();
	});
});
