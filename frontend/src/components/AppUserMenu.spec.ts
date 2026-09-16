import { mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AppUserMenu from './AppUserMenu.vue';

const router = vi.hoisted(() => ({ push: vi.fn(), replace: vi.fn() }));
const authStore = vi.hoisted(() => ({ logout: vi.fn<() => Promise<void>>() }));
const userStore = vi.hoisted(() => ({
	userInfo: null as null | {
		id: number;
		nickname: string;
		avatarUrl: string;
	},
}));
const message = vi.hoisted(() => ({ success: vi.fn(), warning: vi.fn() }));

vi.mock('vue-router', () => ({
	useRouter: () => router,
}));

vi.mock('@/stores/authStore', () => ({
	useAuthStore: () => authStore,
}));

vi.mock('@/stores/userStore', () => ({
	useUserStore: () => userStore,
}));

vi.mock('element-plus', async (importOriginal) => ({
	...(await importOriginal<typeof import('element-plus')>()),
	ElMessage: message,
}));

const ElDropdownItemStub = defineComponent({
	emits: ['click'],
	template: '<button class="dropdown-item" @click="$emit(\'click\')"><slot /></button>',
});

function mountMenu() {
	return mount(AppUserMenu, {
		global: {
			stubs: {
				ElDropdown: { template: '<div><slot /><slot name="dropdown" /></div>' },
				ElDropdownMenu: { template: '<div><slot /></div>' },
				ElDropdownItem: ElDropdownItemStub,
				ElDialog: {
					props: ['modelValue'],
					template:
						'<div v-if="modelValue" class="dialog-stub"><slot /><slot name="footer" /></div>',
				},
				ElButton: {
					props: ['disabled', 'loading'],
					emits: ['click'],
					template:
						'<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
				},
				AppUserAvatar: true,
				AppThemeSwitcher: true,
				ILucideUserRound: true,
				ILucideSettings: true,
				ILucideLogOut: true,
				ILucideLogIn: true,
			},
		},
	});
}

function buttonByText(wrapper: ReturnType<typeof mountMenu>, text: string) {
	const button = wrapper.findAll('button').find((item) => item.text().includes(text));
	if (!button) throw new Error(`Button not found: ${text}`);
	return button;
}

describe('AppUserMenu', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		userStore.userInfo = null;
		authStore.logout.mockResolvedValue(undefined);
	});

	it('shows the login entry for guests', async () => {
		const wrapper = mountMenu();

		await buttonByText(wrapper, '注册/登录').trigger('click');

		expect(router.push).toHaveBeenCalledWith('/auth/login');
		expect(wrapper.get('.app-header__avatar').attributes('aria-label')).toBe('打开登录菜单');
	});

	it('shows profile actions for authenticated users', async () => {
		userStore.userInfo = { id: 10001, nickname: 'Sanjuu', avatarUrl: '' };
		const wrapper = mountMenu();

		await buttonByText(wrapper, '个人中心').trigger('click');
		await buttonByText(wrapper, '个人资料设置').trigger('click');

		expect(router.push).toHaveBeenNthCalledWith(1, '/users/me');
		expect(router.push).toHaveBeenNthCalledWith(2, '/users/me/settings');
		expect(wrapper.get('.app-header__avatar').attributes('aria-label')).toBe('打开用户菜单');
	});

	it('logs out and redirects to the home page', async () => {
		userStore.userInfo = { id: 10001, nickname: 'Sanjuu', avatarUrl: '' };
		const wrapper = mountMenu();
		await buttonByText(wrapper, '退出登录').trigger('click');

		await buttonByText(wrapper, '确认').trigger('click');
		await vi.waitFor(() => expect(authStore.logout).toHaveBeenCalledTimes(1));

		expect(message.success).toHaveBeenCalledWith('已退出登录');
		expect(router.replace).toHaveBeenCalledWith('/');
		expect(wrapper.find('.dialog-stub').exists()).toBe(false);
	});

	it('still redirects home and warns when the logout request fails', async () => {
		userStore.userInfo = { id: 10001, nickname: 'Sanjuu', avatarUrl: '' };
		authStore.logout.mockRejectedValue(new Error('network failed'));
		const wrapper = mountMenu();
		await buttonByText(wrapper, '退出登录').trigger('click');

		await buttonByText(wrapper, '确认').trigger('click');
		await vi.waitFor(() => expect(message.warning).toHaveBeenCalled());

		expect(message.warning).toHaveBeenCalledWith('退出请求失败，请检查网络后重试');
		expect(router.replace).toHaveBeenCalledWith('/');
	});
});
