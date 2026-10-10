import { mount, flushPromises } from '@vue/test-utils';
import { defineComponent, reactive, nextTick } from 'vue';
import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';
import { getSeoMetadata } from '../api/seoApi';
import { usePageSeo } from './usePageSeo';
import type { SeoMetadata } from '../types/seo';
import { applyPageSeo } from '../utils/pageSeo';

const route = reactive({ path: '/about', meta: { title: '关于' }, matched: [{}], query: {} });
vi.mock('vue-router', () => ({ useRoute: () => route }));
vi.mock('../api/seoApi', () => ({ getSeoMetadata: vi.fn() }));

const metadata: SeoMetadata = {
	title: '文章标题 - 青禾边',
	description: '摘要 <文字>',
	canonicalUrl: 'https://blog.example.com/about',
	imageUrl: 'https://blog.example.com/cover.png',
	type: 'article',
	publishedAt: '2026-10-08T00:00:00Z',
	updatedAt: '2026-10-08T01:00:00Z',
};

describe('usePageSeo', () => {
	let wrapper: ReturnType<typeof mount>;
	beforeEach(() => {
		vi.clearAllMocks();
		route.path = '/about';
		route.matched = [{}];
		route.query = {};
		document.head.querySelectorAll('[data-seo]').forEach((element) => element.remove());
		vi.mocked(getSeoMetadata).mockResolvedValue(metadata);
	});
	afterEach(() => {
		wrapper?.unmount();
		document.head.querySelectorAll('[data-seo]').forEach((element) => element.remove());
	});

	function start() {
		wrapper = mount(
			defineComponent({
				setup: () => {
					usePageSeo();
					return () => null;
				},
			}),
		);
	}

	it('applies canonical and escaped metadata for internal public page navigation', async () => {
		start();
		await flushPromises();
		expect(document.title).toBe(metadata.title);
		expect(document.querySelector('link[rel=canonical]')?.getAttribute('href')).toBe(
			metadata.canonicalUrl,
		);
		expect(document.querySelector('meta[name=description]')?.getAttribute('content')).toBe(
			'摘要 <文字>',
		);
		expect(document.querySelector('meta[property="og:image"]')?.getAttribute('content')).toBe(
			metadata.imageUrl,
		);
	});

	it('clears article tags and avoids requesting private page metadata', async () => {
		start();
		await flushPromises();
		route.path = '/auth/login';
		await nextTick();
		expect(document.querySelector('link[rel=canonical]')).toBeNull();
		expect(document.querySelector('meta[property="article:published_time"]')).toBeNull();
		expect(document.querySelector('meta[name=robots]')?.getAttribute('content')).toBe(
			'noindex, follow',
		);
		expect(getSeoMetadata).toHaveBeenCalledTimes(1);
	});

	it('ignores stale responses after navigation', async () => {
		let resolve!: (data: SeoMetadata) => void;
		vi.mocked(getSeoMetadata).mockImplementationOnce(
			() =>
				new Promise((done) => {
					resolve = done;
				}),
		);
		start();
		route.path = '/messages';
		await flushPromises();
		resolve({ ...metadata, title: '过期文章结果' });
		await flushPromises();
		expect(document.title).not.toBe('过期文章结果');
	});

	it('preserves matching server tags on startup without another SEO request', async () => {
		applyPageSeo(metadata);
		const canonical = document.querySelector('link[rel=canonical]');
		start();
		await flushPromises();

		expect(getSeoMetadata).not.toHaveBeenCalled();
		expect(document.querySelector('link[rel=canonical]')).toBe(canonical);
		expect(document.title).toBe(metadata.title);
	});

	it('does not request article metadata or reset it on comment query changes', async () => {
		route.path = '/articles/34';
		applyPageSeo({ ...metadata, canonicalUrl: 'https://blog.example.com/articles/34' });
		start();
		await flushPromises();
		route.query = { replyId: '51' };
		await nextTick();

		expect(getSeoMetadata).not.toHaveBeenCalled();
		expect(document.title).toBe(metadata.title);
		expect(document.querySelector('meta[property="article:published_time"]')).not.toBeNull();
		route.path = '/articles/35';
		await nextTick();
		expect(getSeoMetadata).not.toHaveBeenCalled();
		expect(document.querySelector('link[rel=canonical]')).toBeNull();
	});

	it('waits for initial navigation instead of clearing article HTML tags on the placeholder route', async () => {
		route.matched = [];
		route.path = '/';
		applyPageSeo({ ...metadata, canonicalUrl: 'https://blog.example.com/articles/34' });
		start();
		expect(document.title).toBe(metadata.title);
		route.path = '/articles/34';
		route.matched = [{}];
		await flushPromises();
		expect(document.title).toBe(metadata.title);
		expect(getSeoMetadata).not.toHaveBeenCalled();
	});

	it('keeps unavailable pages unindexed without surfacing an extra error', async () => {
		vi.mocked(getSeoMetadata).mockRejectedValue(new Error('404'));
		start();
		await flushPromises();
		expect(document.querySelector('link[rel=canonical]')).toBeNull();
		expect(document.querySelector('meta[name=robots]')?.getAttribute('content')).toBe(
			'noindex, follow',
		);
	});
});
