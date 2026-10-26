import { flushPromises, mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter, type LocationQuery } from 'vue-router';
import { routes } from '@/router/routes';
import { login } from '../api/authApi';
import LoginPage from './LoginPage.vue';

const router = vi.hoisted(() => ({ replace: vi.fn(), resolve: vi.fn() }));
const route = vi.hoisted(() => ({ query: {} as LocationQuery }));
const setAccessToken = vi.hoisted(() => vi.fn());
const setUserInfo = vi.hoisted(() => vi.fn());
const showSuccess = vi.hoisted(() => vi.fn());

vi.mock('vue-router', async (importOriginal) => ({
	...(await importOriginal<typeof import('vue-router')>()),
	useRouter: () => router,
	useRoute: () => route,
}));

vi.mock('../api/authApi', () => ({
	login: vi.fn(),
}));

vi.mock('@/stores/authStore', () => ({
	useAuthStore: () => ({ setAccessToken }),
}));

vi.mock('@/stores/userStore', () => ({
	useUserStore: () => ({ setUserInfo }),
}));

vi.mock('element-plus', async (importOriginal) => ({
	...(await importOriginal<typeof import('element-plus')>()),
	ElMessage: { success: showSuccess },
}));

const wrappers: ReturnType<typeof mount>[] = [];
const testRouter = createRouter({ history: createMemoryHistory(), routes });

function mountPage() {
	const wrapper = mount(LoginPage, {
		global: {
			stubs: {
				IEpUser: true,
				IEpLock: true,
			},
		},
	});
	wrappers.push(wrapper);
	return wrapper;
}

describe('LoginPage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		route.query = {};
		router.resolve.mockImplementation(testRouter.resolve);
		vi.mocked(login).mockResolvedValue({
			accessToken: 'access-token',
			accessTokenExpiresAt: '2026-09-15T12:00:00+08:00',
			refreshTokenExpiresAt: '2026-09-22T12:00:00+08:00',
			tokenType: 'Bearer',
			user: {
				id: 10001,
				username: 'demo_user',
				nickname: '测试用户',
				email: 'demo@example.com',
				avatarUrl: '',
				bio: '',
				role: 'USER',
				status: 'ACTIVE',
			},
		});
	});

	afterEach(() => wrappers.splice(0).forEach((wrapper) => wrapper.unmount()));

	it('does not log in when the form is submitted with invalid values', async () => {
		const wrapper = mountPage();

		expect(wrapper.get<HTMLButtonElement>('button[type="submit"]').element.disabled).toBe(true);
		await wrapper.get('form').trigger('submit');

		expect(login).not.toHaveBeenCalled();
	});

	it('logs in when a valid form is submitted', async () => {
		const wrapper = mountPage();
		const inputs = wrapper.findAll('input');

		await inputs[0].setValue('demo_user');
		await inputs[1].setValue('Passw0rd!');
		await flushPromises();
		await wrapper.get('form').trigger('submit');
		await flushPromises();

		expect(login).toHaveBeenCalledTimes(1);
		expect(login).toHaveBeenCalledWith({
			account: 'demo_user',
			password: 'Passw0rd!',
		});
		expect(setAccessToken).toHaveBeenCalledWith('access-token');
		expect(router.replace).toHaveBeenCalledWith('/');
	});

	it.each([
		['/articles/34?replyId=51#article-comments', '/articles/34?replyId=51#article-comments'],
		['/articles?keyword=Vue', '/articles?keyword=Vue'],
		['//example.com', '/'],
		['/missing-page', '/'],
		['/auth/register', '/'],
	])('returns to a valid target after login with redirect=%s', async (redirect, expected) => {
		route.query = { redirect };
		const wrapper = mountPage();
		const inputs = wrapper.findAll('input');
		await inputs[0].setValue('demo_user');
		await inputs[1].setValue('Passw0rd!');
		await flushPromises();
		await wrapper.get('form').trigger('submit');
		await flushPromises();
		expect(router.replace).toHaveBeenCalledWith(expected);
		expect(setUserInfo).toHaveBeenCalled();
	});

	it('keeps the target when switching to registration', async () => {
		const redirect = '/articles/34?replyId=51#article-comments';
		route.query = { redirect };
		const wrapper = mountPage();
		await wrapper
			.findAll('.el-link')
			.find((link) => link.text() === '没有账号？立即注册')!
			.trigger('click');
		expect(router.replace).toHaveBeenCalledWith({
			path: '/auth/register',
			query: { redirect },
		});
	});
});
