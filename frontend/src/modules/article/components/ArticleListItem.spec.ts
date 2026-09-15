import { mount, RouterLinkStub } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ArticleListItem from './ArticleListItem.vue';
import type { PublicArticleListItem } from '../types/article';

const mediaQueryState = vi.hoisted(() => ({ isMobile: false }));

vi.mock('@vueuse/core', async (importOriginal) => {
	const { computed } = await import('vue');
	return {
		...(await importOriginal<typeof import('@vueuse/core')>()),
		useMediaQuery: vi.fn(() => computed(() => mediaQueryState.isMobile)),
	};
});

function article(overrides: Partial<PublicArticleListItem> = {}): PublicArticleListItem {
	return {
		id: 40001,
		title: 'Spring Boot 双 Token 登录实践',
		summary: '本文记录双 Token 的实现思路与接口设计',
		coverUrl: '',
		isTop: false,
		publishedAt: '2026-04-22T23:00:00+08:00',
		viewCount: 128,
		category: {
			id: 21001,
			name: 'Java',
			level: 2,
			parent: { id: 20001, name: '技术' },
		},
		tags: [],
		...overrides,
	};
}

function mountItem(articleData: PublicArticleListItem) {
	return mount(ArticleListItem, {
		props: { article: articleData },
		global: {
			stubs: {
				RouterLink: RouterLinkStub,
				AppImage: true,
				AppTagCapsule: true,
			},
		},
	});
}

describe('ArticleListItem', () => {
	beforeEach(() => {
		mediaQueryState.isMobile = false;
	});

	it('renders the safe search highlight fields returned by the backend', () => {
		const wrapper = mountItem(
			article({
				highlightedTitle:
					'<mark class="article-search-highlight">Spring Boot</mark> 双 Token 登录实践',
				searchSnippet:
					'本文记录 <mark class="article-search-highlight">双 Token</mark> 的实现思路',
			}),
		);

		expect(wrapper.findAll('mark.article-search-highlight')).toHaveLength(2);
		expect(wrapper.find('.article-list-item-title').text()).toContain('Spring Boot');
	});

	it('renders the fallback title as text instead of raw HTML', () => {
		const wrapper = mountItem(article({ title: '<img src=x onerror=alert(1)>' }));

		expect(wrapper.find('.article-list-item-title').text()).toBe(
			'<img src=x onerror=alert(1)>',
		);
		expect(wrapper.find('.article-list-item-title img').exists()).toBe(false);
	});

	it('marks the cover image for the mobile layout', () => {
		mediaQueryState.isMobile = true;
		const wrapper = mountItem(article());

		expect(wrapper.find('.article-list-item-image').exists()).toBe(true);
	});
});
