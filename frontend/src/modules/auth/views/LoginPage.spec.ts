import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { login } from '../api/authApi';
import LoginPage from './LoginPage.vue';

const router = vi.hoisted(() => ({ replace: vi.fn() }));
const setAccessToken = vi.hoisted(() => vi.fn());
const setUserInfo = vi.hoisted(() => vi.fn());
const showSuccess = vi.hoisted(() => vi.fn());

vi.mock('vue-router', () => ({
	useRouter: () => router,
	useRoute: () => ({ query: {} }),
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

function mountPage() {
	return mount(LoginPage, {
		global: {
			stubs: {
				IEpUser: true,
				IEpLock: true,
			},
		},
	});
}

describe('LoginPage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
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
});
