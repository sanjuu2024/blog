import { flushPromises, mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { getPrivacyPolicy } from '@/modules/privacy/api/privacyApi';
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

vi.mock('@/modules/privacy/api/privacyApi', () => ({
	getPrivacyPolicy: vi.fn(),
}));

vi.mock('element-plus', async (importOriginal) => ({
	...(await importOriginal<typeof import('element-plus')>()),
	ElMessage: { success: showSuccess },
}));

const wrappers: ReturnType<typeof mount>[] = [];

function mountPage() {
	const wrapper = mount(RegisterPage, {
		global: {
			stubs: {
				IEpUser: true,
				IEpMessage: true,
				IEpLock: true,
				ILucideLoader: true,
			},
		},
	});
	wrappers.push(wrapper);
	return wrapper;
}

describe('RegisterPage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(register).mockResolvedValue(null);
		vi.mocked(getPrivacyPolicy).mockResolvedValue({
			version: 'sha256:first',
			contentHtml: '<h1>隐私政策</h1><p>服务端政策正文</p>',
			contentText: '隐私政策 服务端政策正文',
		});
	});

	afterEach(() => {
		wrappers.splice(0).forEach((wrapper) => wrapper.unmount());
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

	it('loads the latest privacy policy whenever the dialog opens without submitting registration', async () => {
		const wrapper = mountPage();
		const privacyLink = wrapper.findAll('.el-link').find((link) => link.text() === '隐私政策')!;

		expect(getPrivacyPolicy).not.toHaveBeenCalled();
		await privacyLink.trigger('click');
		await flushPromises();

		expect(wrapper.get('[role="dialog"]').text()).toContain('服务端政策正文');
		expect(register).not.toHaveBeenCalled();

		await wrapper.get('.el-dialog__footer button').trigger('click');
		await flushPromises();
		vi.mocked(getPrivacyPolicy).mockResolvedValueOnce({
			version: 'sha256:second',
			contentHtml: '<p>更新后的政策正文</p>',
			contentText: '更新后的政策正文',
		});
		await privacyLink.trigger('click');
		await flushPromises();

		expect(getPrivacyPolicy).toHaveBeenCalledTimes(2);
		expect(wrapper.get('[role="dialog"]').text()).toContain('更新后的政策正文');
	});

	it('allows retrying a failed policy request in the dialog', async () => {
		vi.mocked(getPrivacyPolicy).mockRejectedValueOnce(new Error('network failed'));
		const wrapper = mountPage();
		await wrapper
			.findAll('.el-link')
			.find((link) => link.text() === '隐私政策')!
			.trigger('click');
		await flushPromises();

		expect(wrapper.get('[role="dialog"]').text()).toContain('隐私政策加载失败');
		await wrapper.get('.privacy-policy-dialog__state button').trigger('click');
		await flushPromises();

		expect(wrapper.get('[role="dialog"]').text()).toContain('服务端政策正文');
		expect(getPrivacyPolicy).toHaveBeenCalledTimes(2);
	});
});
