import { mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminHeader from './AdminHeader.vue';

const router = vi.hoisted(() => ({ push: vi.fn() }));
const route = vi.hoisted(() => ({
	meta: { title: '编辑文章', icon: 'article' },
	matched: [
		{ name: 'AdminLayout', path: '/admin', meta: { title: '后台管理' } },
		{ name: 'AdminArticle', path: '/admin/articles', meta: { title: '文章管理' } },
		{
			name: 'AdminArticleEdit',
			path: '/admin/articles/40001/edit',
			meta: { title: '编辑文章' },
		},
	],
}));

vi.mock('vue-router', async (importOriginal) => ({
	...(await importOriginal<typeof import('vue-router')>()),
	useRoute: () => route,
	useRouter: () => router,
}));

vi.mock('@/utils/getRouteIcon', async () => {
	const { defineComponent } = await import('vue');
	return {
		default: vi.fn(() => defineComponent({ template: '<span class="route-icon-stub" />' })),
	};
});

vi.mock('@/components/AppLogo.vue', () => ({
	default: { template: '<div />' },
}));

vi.mock('@/components/AppUserMenu.vue', () => ({
	default: { template: '<div />' },
}));

const ElDropdownStub = defineComponent({
	name: 'ElDropdown',
	emits: ['command'],
	template: '<div><slot /><slot name="dropdown" /></div>',
});

function mountHeader() {
	return mount(AdminHeader, {
		global: {
			stubs: {
				ElBreadcrumb: { template: '<nav><slot /></nav>' },
				ElBreadcrumbItem: { template: '<span><slot /></span>' },
				ElIcon: { template: '<span><slot /></span>' },
				ElDropdown: ElDropdownStub,
				ElDropdownMenu: { template: '<div><slot /></div>' },
				ElDropdownItem: {
					props: ['command'],
					template: '<span :data-command="command"><slot /></span>',
				},
				AppLogo: true,
				AppUserMenu: true,
				ILucideList: true,
			},
		},
	});
}

describe('AdminHeader', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('renders the current page title and all admin navigation entries', () => {
		const wrapper = mountHeader();
		const mobileNavigation = wrapper.get('.admin-header__mobile-nav');

		expect(wrapper.get('.admin-header__mobile-title').text()).toContain('编辑文章');
		for (const label of [
			'前台',
			'仪表盘',
			'用户管理',
			'文章管理',
			'分类管理',
			'标签管理',
			'评论管理',
			'留言管理',
			'操作审计',
		]) {
			expect(mobileNavigation.text()).toContain(label);
		}
	});

	it('navigates to the selected admin menu item', async () => {
		const wrapper = mountHeader();

		wrapper.getComponent(ElDropdownStub).vm.$emit('command', '/admin/comments');
		await wrapper.vm.$nextTick();

		expect(router.push).toHaveBeenCalledWith('/admin/comments');
	});
});
