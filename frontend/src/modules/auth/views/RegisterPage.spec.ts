import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { register } from '../api/authApi';
import RegisterPage from './RegisterPage.vue';

const router = vi.hoisted(() => ({ replace: vi.fn() }));
const showSuccess = vi.hoisted(() => vi.fn());

vi.mock('vue-router', () => ({
	useRouter: () => router,
}));

vi.mock('../api/authApi', () => ({
	register: vi.fn(),
}));

vi.mock('element-plus', async (importOriginal) => ({
	...(await importOriginal<typeof import('element-plus')>()),
	ElMessage: { success: showSuccess },
}));

function mountPage() {
	return mount(RegisterPage, {
		global: {
			stubs: {
				IEpUser: true,
				IEpMessage: true,
				IEpLock: true,
			},
		},
	});
}

describe('RegisterPage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(register).mockResolvedValue(null);
	});

	it('does not register when the form is submitted with invalid values', async () => {
		const wrapper = mountPage();

		expect(wrapper.get<HTMLButtonElement>('button[type="submit"]').element.disabled).toBe(true);
		await wrapper.get('form').trigger('submit');

		expect(register).not.toHaveBeenCalled();
	});

	it('registers when a valid form is submitted', async () => {
		const wrapper = mountPage();
		const inputs = wrapper.findAll('input');

		await inputs[0].setValue('demo_user');
		await inputs[1].setValue('demo@example.com');
		await inputs[2].setValue('Passw0rd!');
		await flushPromises();
		await wrapper.get('form').trigger('submit');
		await flushPromises();

		expect(register).toHaveBeenCalledTimes(1);
		expect(register).toHaveBeenCalledWith({
			username: 'demo_user',
			email: 'demo@example.com',
			password: 'Passw0rd!',
		});
		expect(router.replace).toHaveBeenCalledWith('/auth/login');
		expect(showSuccess).toHaveBeenCalledWith('注册成功，请登录');
	});
});
