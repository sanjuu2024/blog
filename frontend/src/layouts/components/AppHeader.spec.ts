import { mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AppHeader from './AppHeader.vue';

const router = vi.hoisted(() => ({ push: vi.fn() }));
const userStore = vi.hoisted(() => ({
	userInfo: { role: 'ADMIN' } as { role: string } | null,
}));

vi.mock('vue-router', async (importOriginal) => ({
	...(await importOriginal<typeof import('vue-router')>()),
	useRouter: () => router,
}));

vi.mock('@/stores/userStore', () => ({
	useUserStore: () => userStore,
}));

vi.mock('@/components/AppLogo.vue', () => ({
	default: { template: '<div />' },
}));

vi.mock('@/components/AppUserMenu.vue', () => ({
	default: { template: '<div />' },
}));

vi.mock('@vueuse/core', async (importOriginal) => {
	const { ref } = await import('vue');
	return {
		...(await importOriginal<typeof import('@vueuse/core')>()),
		useMediaQuery: vi.fn(() => ref(true)),
	};
});

const ElDropdownStub = defineComponent({
	name: 'ElDropdown',
	emits: ['command'],
	template: '<div><slot /><slot name="dropdown" /></div>',
});

function mountHeader() {
	return mount(AppHeader, {
		global: {
			stubs: {
				RouterLink: { props: ['to'], template: '<a><slot /></a>' },
				AppLogo: true,
				AppUserMenu: {
					props: ['pxRem'],
					template: '<div class="user-menu-stub" :data-px-rem="pxRem" />',
				},
				ElDropdown: ElDropdownStub,
				ElDropdownMenu: { template: '<div><slot /></div>' },
				ElDropdownItem: {
					props: ['command'],
					template: '<span :data-command="command"><slot /></span>',
				},
				ILucideList: true,
			},
		},
	});
}

describe('AppHeader', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		userStore.userInfo = { role: 'ADMIN' };
	});

	it('renders public and admin entries in the mobile navigation', () => {
		const wrapper = mountHeader();
		const mobileNavigation = wrapper.get('.app-header__mobile-nav');

		for (const label of ['首页', '文章', '分类', '留言', '关于', '后台']) {
			expect(mobileNavigation.text()).toContain(label);
		}
	});

	it('navigates to the selected mobile menu item', async () => {
		const wrapper = mountHeader();

		wrapper.getComponent(ElDropdownStub).vm.$emit('command', '/articles');
		await wrapper.vm.$nextTick();

		expect(router.push).toHaveBeenCalledWith('/articles');
	});
});
