import { mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { createMemoryHistory, createRouter, type RouteRecordRaw, type Router } from 'vue-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { routes } from '@/router/routes';
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

function mountHeader(navigationRouter?: Router) {
	return mount(AppHeader, {
		global: {
			plugins: navigationRouter ? [navigationRouter] : [],
			stubs: {
				RouterLink: navigationRouter
					? false
					: { props: ['to'], template: '<a><slot /></a>' },
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

// 保留真实路由匹配，只替换页面组件，避免导航测试加载完整业务页面。
function stubPageComponents(records: RouteRecordRaw[]): RouteRecordRaw[] {
	return records.map((record) => {
		const stubbed = { ...record };
		if ('component' in stubbed) stubbed.component = defineComponent(() => () => null);
		if (stubbed.children) stubbed.children = stubPageComponents(stubbed.children);
		return stubbed;
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

		for (const label of ['首页', '文章', '归档', '留言', '关于', '后台']) {
			expect(mobileNavigation.text()).toContain(label);
		}
	});

	it('navigates to the selected mobile menu item', async () => {
		const wrapper = mountHeader();

		wrapper.getComponent(ElDropdownStub).vm.$emit('command', '/articles');
		await wrapper.vm.$nextTick();

		expect(router.push).toHaveBeenCalledWith('/articles');
	});

	describe('navigation highlighting', () => {
		let wrapper: ReturnType<typeof mountHeader>;
		let navigationRouter: Router;

		beforeEach(() => {
			navigationRouter = createRouter({
				history: createMemoryHistory(),
				routes: stubPageComponents(routes),
			});
		});

		afterEach(() => wrapper?.unmount());

		it.each(['/', '/?from=mail#content', '/home?from=mail#content'])(
			'highlights the homepage after navigation to %s',
			async (path) => {
				await navigationRouter.push(path);
				wrapper = mountHeader(navigationRouter);

				const homeLink = wrapper.get('.app-header__link[href="/"]');
				expect(homeLink.classes()).toContain('router-link-active');
				expect(homeLink.attributes('aria-current')).toBe('page');
				expect(wrapper.findAll('.app-header__link.router-link-active')).toHaveLength(1);
			},
		);

		it.each([
			['/articles', '文章'],
			['/articles?keyword=Vue', '文章'],
			['/archives?mode=tag&tagId=30001', '归档'],
			['/messages', '留言'],
			['/links', '友链'],
			['/about', '关于'],
			['/articles/40001', null],
			['/users/me', null],
			['/admin/articles/list', '后台'],
		])('clears homepage highlighting after navigation to %s', async (path, activeLabel) => {
			await navigationRouter.push('/');
			wrapper = mountHeader(navigationRouter);
			const homeLink = wrapper.get('.app-header__link[href="/"]');
			expect(homeLink.classes()).toContain('router-link-active');

			await navigationRouter.push(path);
			await wrapper.vm.$nextTick();

			expect(homeLink.classes()).not.toContain('router-link-active');
			expect(homeLink.attributes('aria-current')).toBeUndefined();
			expect(
				wrapper.findAll('.app-header__link.router-link-active').map((link) => link.text()),
			).toEqual(activeLabel ? [activeLabel] : []);

			await navigationRouter.push({ name: 'Home' });
			await wrapper.vm.$nextTick();
			expect(homeLink.classes()).toContain('router-link-active');
		});
	});
});
