import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { createMemoryHistory, createRouter, type RouteRecordRaw } from 'vue-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from '@/App.vue';
import MainLayout from '@/layouts/MainLayout.vue';
import AppHeader from '@/layouts/components/AppHeader.vue';
import AppFooter from '@/layouts/components/AppFooter.vue';
import { routes } from '@/router/routes';
import { getArticleDetails } from '../api/articleApi';
import ArticleDetailPage from './ArticleDetailPage.vue';

vi.mock('../api/articleApi', () => ({ getArticleDetails: vi.fn() }));
vi.mock('@/utils/prism', () => ({ highlightCodeUnder: vi.fn() }));
vi.mock('canvas-confetti', () => ({ default: vi.fn() }));
vi.mock('element-plus', async (importOriginal) => ({
	...(await importOriginal<typeof import('element-plus')>()),
	ElMessage: { error: vi.fn() },
}));
vi.mock('@/stores/userStore', () => ({ useUserStore: () => ({ userInfo: null }) }));
vi.mock('@/components/AppLogo.vue', () => ({ default: { template: '<div />' } }));
vi.mock('@/components/AppUserMenu.vue', () => ({ default: { template: '<div />' } }));

// 使用真实布局、文章页面和错误处理，只替换其他路由组件及无关交互组件。
function stubOtherPages(records: RouteRecordRaw[]): RouteRecordRaw[] {
	return records.map((record) => {
		const stubbed = { ...record };
		if ('component' in stubbed) {
			stubbed.component =
				record.name === 'MainLayout'
					? MainLayout
					: record.name === 'ArticleDetail'
						? ArticleDetailPage
						: defineComponent(() => () => null);
		}
		if (stubbed.children) stubbed.children = stubOtherPages(stubbed.children);
		return stubbed;
	});
}

describe('article error page in the public layout', () => {
	let wrapper: ReturnType<typeof mount>;

	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(getArticleDetails).mockRejectedValue({
			isAxiosError: true,
			response: { status: 404, data: { code: 103001, message: '文章不存在' } },
		});
		document.head.innerHTML =
			'<title>文章不存在 - 青禾边</title><meta name="robots" content="noindex, follow" data-seo="true">';
	});

	afterEach(() => {
		wrapper?.unmount();
		document.head.querySelectorAll('[data-seo]').forEach((element) => element.remove());
	});

	it.each(
		(['direct', 'internal'] as const).flatMap((entry) =>
			['333', '040001', '40001.0', '4e4', '0x22'].map((id) => ({ entry, id })),
		),
	)(
		'shows the existing article error for $id after $entry navigation without leaving the layout',
		async ({ entry, id }) => {
			const router = createRouter({
				history: createMemoryHistory(),
				routes: stubOtherPages(routes),
			});
			const target = `/articles/${id}?replyId=51#article-comments`;
			await router.push(entry === 'direct' ? target : '/users/me');
			wrapper = mount(App, {
				global: {
					plugins: [router],
					stubs: {
						ElConfigProvider: { props: ['locale'], template: '<slot />' },
						ElDropdown: { template: '<div><slot /></div>' },
						AppLogo: true,
						AppUserMenu: true,
						AppBacktop: true,
					},
				},
			});
			if (entry === 'internal') await router.push(target);
			await flushPromises();

			expect(wrapper.findComponent(AppHeader).exists()).toBe(true);
			expect(wrapper.findComponent(AppFooter).exists()).toBe(true);
			expect(wrapper.get('.app-main .article-content-wrapper').text()).toBe(
				id === '333' ? '文章不存在' : '文章 ID 不合法',
			);
			expect(wrapper.find('.not-found-page').exists()).toBe(false);
			expect(wrapper.find('#article-comments').exists()).toBe(false);
			expect(router.currentRoute.value.fullPath).toBe(target);
			if (id === '333') expect(getArticleDetails).toHaveBeenCalledExactlyOnceWith(333);
			else expect(getArticleDetails).not.toHaveBeenCalled();
			expect(document.querySelector('meta[name=robots]')?.getAttribute('content')).toBe(
				'noindex, follow',
			);
			expect(document.querySelector('link[rel=canonical], meta[property^="og:"]')).toBeNull();
		},
	);
});
