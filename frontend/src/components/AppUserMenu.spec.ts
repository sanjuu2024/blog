import { mount } from '@vue/test-utils';
import { computed, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useRoute, useRouter } from 'vue-router';
import { useNotificationUnread } from '@/modules/notification/composables/useNotifications';
import AppUserMenu from './AppUserMenu.vue';

vi.mock('vue-router', async (importOriginal) => ({
	...(await importOriginal<typeof import('vue-router')>()),
	useRouter: vi.fn(),
	useRoute: vi.fn(),
}));
vi.mock('@/stores/authStore', () => ({ useAuthStore: () => ({ logout: vi.fn() }) }));
const userStore = vi.hoisted(() => ({
	userInfo: null as { id: number; nickname: string; avatarUrl: string } | null,
}));
vi.mock('@/stores/userStore', () => ({ useUserStore: () => userStore }));
vi.mock('@/modules/notification/composables/useNotifications', () => ({
	useNotificationUnread: vi.fn(),
}));

describe('AppUserMenu', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		userStore.userInfo = { id: 10001, nickname: '测试用户', avatarUrl: '' };
		vi.mocked(useRoute).mockReturnValue({ fullPath: '/articles' } as never);
	});
	it('shows the unread dot by the menu icon and opens the dialog', async () => {
		const push = vi.fn();
		vi.mocked(useRouter).mockReturnValue({ push } as unknown as ReturnType<typeof useRouter>);
		vi.mocked(useNotificationUnread).mockReturnValue({
			hasUnread: computed(() => true),
			loadUnreadCount: vi.fn(),
			clearUnreadCount: vi.fn(),
			unreadCount: ref({ total: 1, reply: 1, adminMessage: 0 }),
		});
		const wrapper = mount(AppUserMenu, {
			global: {
				stubs: {
					ElDropdown: { template: '<div><slot /><slot name="dropdown" /></div>' },
					ElDropdownMenu: { template: '<div><slot /></div>' },
					ElDropdownItem: { template: '<button><slot /></button>' },
					ElBadge: {
						props: ['isDot', 'hidden'],
						template:
							'<span data-testid="menu-badge" :data-dot="isDot"><slot /></span>',
					},
					ElDialog: true,
					NotificationDialog: {
						props: ['modelValue'],
						template: '<div data-testid="notification-dialog">{{ modelValue }}</div>',
					},
					AppUserAvatar: true,
					AppThemeSwitcher: true,
					ILucideBell: true,
				},
			},
		});

		expect(wrapper.find('[data-testid="menu-badge"]').attributes('data-dot')).toBe('true');
		await wrapper
			.findAll('button')
			.find((button) => button.text() === '通知')
			?.trigger('click');

		expect(wrapper.find('[data-testid="notification-dialog"]').text()).toBe('true');
		expect(push).not.toHaveBeenCalled();
		wrapper.unmount();
	});

	it('passes the current query and hash to login from the guest menu', async () => {
		userStore.userInfo = null;
		const redirect = '/articles/34?replyId=51#article-comments';
		const push = vi.fn();
		vi.mocked(useRoute).mockReturnValue({ fullPath: redirect } as never);
		vi.mocked(useRouter).mockReturnValue({ push } as never);
		vi.mocked(useNotificationUnread).mockReturnValue({
			hasUnread: computed(() => false),
			loadUnreadCount: vi.fn(),
			clearUnreadCount: vi.fn(),
			unreadCount: ref({ total: 0, reply: 0, adminMessage: 0 }),
		});
		const wrapper = mount(AppUserMenu, {
			global: {
				stubs: {
					ElDropdown: { template: '<div><slot /><slot name="dropdown" /></div>' },
					ElDropdownMenu: { template: '<div><slot /></div>' },
					ElDropdownItem: { template: '<button><slot /></button>' },
					ElDialog: true,
					NotificationDialog: true,
					AppThemeSwitcher: true,
				},
			},
		});
		await wrapper
			.findAll('button')
			.find((button) => button.text() === '注册/登录')!
			.trigger('click');
		expect(push).toHaveBeenCalledExactlyOnceWith({ path: '/auth/login', query: { redirect } });
		wrapper.unmount();
	});
});
