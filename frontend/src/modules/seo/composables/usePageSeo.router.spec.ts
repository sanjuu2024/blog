import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { createMemoryHistory, createRouter, type RouteRecordRaw } from 'vue-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { routes } from '@/router/routes';
import { getAuthRedirect } from '@/modules/auth/utils/redirect';
import { getSeoMetadata } from '../api/seoApi';
import type { SeoMetadata } from '../types/seo';
import { applyPageSeo } from '../utils/pageSeo';
import { usePageSeo } from './usePageSeo';

vi.mock('../api/seoApi', () => ({ getSeoMetadata: vi.fn() }));

const metadata: SeoMetadata = {
	title: '青禾边',
	description: '首页描述',
	canonicalUrl: 'https://blog.example.com/',
	imageUrl: null,
	type: 'website',
	publishedAt: null,
	updatedAt: null,
};

// 保留真实路由及重定向，只替换页面组件，避免加载与首页 SEO 无关的业务模块。
function stubPageComponents(records: RouteRecordRaw[]): RouteRecordRaw[] {
	return records.map((record) => {
		const stubbed = { ...record };
		if ('component' in stubbed) stubbed.component = defineComponent(() => () => null);
		if (stubbed.children) stubbed.children = stubPageComponents(stubbed.children);
		return stubbed;
	});
}

describe('homepage routing and SEO', () => {
	let wrapper: ReturnType<typeof mount>;
	let router: ReturnType<typeof createRouter>;

	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(getSeoMetadata).mockResolvedValue(metadata);
		router = createRouter({
			history: createMemoryHistory(),
			routes: stubPageComponents(routes),
		});
	});

	afterEach(() => {
		wrapper?.unmount();
		document.head.querySelectorAll('[data-seo]').forEach((element) => element.remove());
	});

	function start() {
		wrapper = mount(
			defineComponent({
				setup() {
					usePageSeo();
					return () => null;
				},
			}),
			{ global: { plugins: [router] } },
		);
	}

	it.each(['/', '/home?from=mail#content'])(
		'preserves server homepage metadata after initial navigation to %s',
		async (target) => {
			applyPageSeo(metadata);
			const canonical = document.querySelector('link[rel=canonical]');
			start();
			await router.replace(target);
			await flushPromises();

			expect(router.currentRoute.value.name).toBe('Home');
			expect(router.currentRoute.value.path).toBe('/');
			expect(router.currentRoute.value.fullPath).toBe(target.replace('/home', '/'));
			expect(getSeoMetadata).not.toHaveBeenCalled();
			expect(document.querySelector('link[rel=canonical]')).toBe(canonical);
			expect(document.title).toBe(metadata.title);
			expect(document.querySelector('meta[name=robots]')?.getAttribute('content')).toBe(
				'index, follow',
			);
			expect(document.querySelector('meta[property="og:url"]')?.getAttribute('content')).toBe(
				metadata.canonicalUrl,
			);
		},
	);

	it.each(['/', '/home?from=login#content'])(
		'normalizes the login return target %s and loads root metadata',
		async (target) => {
			await router.replace('/auth/login');
			start();
			await flushPromises();
			expect(document.querySelector('meta[name=robots]')?.getAttribute('content')).toBe(
				'noindex, follow',
			);

			await router.replace(getAuthRedirect(router, target) || '/');
			await flushPromises();

			expect(router.currentRoute.value.name).toBe('Home');
			expect(router.currentRoute.value.fullPath).toBe(target.replace('/home', '/'));
			expect(getSeoMetadata).toHaveBeenCalledExactlyOnceWith('/');
			expect(document.querySelector('link[rel=canonical]')?.getAttribute('href')).toBe(
				metadata.canonicalUrl,
			);
			expect(document.querySelector('meta[name=robots]')?.getAttribute('content')).toBe(
				'index, follow',
			);
			expect(router.resolve({ name: 'Home' }).path).toBe('/');
		},
	);
});
