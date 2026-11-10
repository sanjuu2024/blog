import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { createMemoryHistory, createRouter, RouterView } from 'vue-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppMain from '@/layouts/components/AppMain.vue';
import AppLoadMoreTrigger from '@/components/AppLoadMoreTrigger.vue';
import { getEnabledCategories } from '@/modules/category/api/categoryApi';
import { getEnabledTags } from '@/modules/tag/api/tagApi';
import {
	articleListScrollPending,
	prepareArticleListScroll,
	restoreArticleListScroll,
	setPageViewPending,
} from '@/router/articleListScroll';
import { listArticles } from '../api/articleApi';
import type { PublicArticleListItem, PublicArticlePageData } from '../types/article';
import ArticleListPage from './ArticleListPage.vue';

vi.mock('../api/articleApi', () => ({ listArticles: vi.fn() }));
vi.mock('@/modules/category/api/categoryApi', () => ({ getEnabledCategories: vi.fn() }));
vi.mock('@/modules/tag/api/tagApi', () => ({ getEnabledTags: vi.fn() }));

enableAutoUnmount(afterEach);

const detailMounted = vi.fn();
const DetailPage = defineComponent({
	name: 'TestArticleDetail',
	setup() {
		detailMounted();
	},
	template: '<div class="test-detail">详情</div>',
});

function article(id: number): PublicArticleListItem {
	return {
		id,
		title: `文章 ${id}`,
		summary: '',
		coverUrl: '',
		isTop: false,
		publishedAt: '2026-11-03T00:00:00Z',
		viewCount: 0,
		likeCount: 0,
		commentCount: 0,
		category: { id: 2, name: 'Java', level: 2, parent: { id: 1, name: '技术' } },
		tags: [],
	};
}

function page(id: number, pageNum = 1): PublicArticlePageData {
	return {
		records: [article(id)],
		pageNum,
		pageSize: 10,
		total: 30,
		totalPages: 3,
		hasNext: true,
	};
}

async function mountList(
	path = '/articles?keyword=Vue&categoryId=2&tagIds=1',
	withTransition = false,
) {
	const router = createRouter({
		history: createMemoryHistory(),
		routes: [
			{
				path: '/',
				component: AppMain,
				children: [
					{ path: 'articles', name: 'ArticleList', component: ArticleListPage },
					{ path: 'articles/:articleId', name: 'ArticleDetail', component: DetailPage },
				],
			},
		],
		scrollBehavior: withTransition
			? (to) => {
					if (to.name !== 'ArticleList') return false;
					return restoreArticleListScroll(to.fullPath, { left: 0, top: 1500 });
				}
			: undefined,
	});
	if (withTransition) {
		router.beforeResolve((to) => prepareArticleListScroll(to.name === 'ArticleList'));
	}
	await router.push(path);
	const wrapper = mount(RouterView, {
		attachTo: document.body,
		global: {
			plugins: [router],
			stubs: {
				...(withTransition ? { transition: false } : {}),
				ArticleListItem: {
					props: ['article'],
					template: '<div class="test-article">{{ article.title }}</div>',
				},
				ArticleFilterPanel: true,
				AppLoadMoreTrigger: true,
				AppBacktop: true,
				ElInput: {
					props: ['modelValue'],
					emits: ['update:modelValue'],
					template:
						'<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
				},
				ElButton: { template: '<button><slot /></button>' },
			},
		},
	});
	await flushPromises();
	return { router, wrapper };
}

describe('ArticleListPage cached navigation', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		prepareArticleListScroll(false);
		setPageViewPending(false);
		vi.mocked(getEnabledCategories).mockResolvedValue([]);
		vi.mocked(getEnabledTags).mockResolvedValue([]);
		vi.mocked(listArticles).mockResolvedValue(page(1));
	});

	afterEach(() => {
		prepareArticleListScroll(false);
		document.querySelector('[data-article-list-transition-test]')?.remove();
		vi.restoreAllMocks();
	});

	it('retains loaded pages, applied filters and draft input when returning from details', async () => {
		vi.mocked(listArticles).mockResolvedValueOnce(page(1)).mockResolvedValueOnce(page(2, 2));
		const { router, wrapper } = await mountList();
		const originalListElement = wrapper.get('.article-list-page').element;
		await wrapper.get('input').setValue('尚未搜索的文字');
		wrapper.getComponent(AppLoadMoreTrigger).vm.$emit('load-more');
		await flushPromises();
		expect(wrapper.findAll('.test-article')).toHaveLength(2);

		await router.push('/articles/2?replyId=51');
		await flushPromises();
		expect(wrapper.findComponent(ArticleListPage).exists()).toBe(false);
		expect(listArticles).toHaveBeenCalledTimes(2);

		router.back();
		await flushPromises();

		expect(wrapper.get('.article-list-page').element).toBe(originalListElement);
		expect(wrapper.findAll('.test-article').map((row) => row.text())).toEqual([
			'文章 1',
			'文章 2',
		]);
		expect((wrapper.get('input').element as HTMLInputElement).value).toBe('尚未搜索的文字');
		expect(listArticles).toHaveBeenCalledTimes(2);
		expect(getEnabledCategories).toHaveBeenCalledTimes(1);
		expect(getEnabledTags).toHaveBeenCalledTimes(1);
		vi.mocked(listArticles).mockResolvedValueOnce(page(3, 3));
		wrapper.getComponent(AppLoadMoreTrigger).vm.$emit('load-more');
		await flushPromises();
		expect(listArticles).toHaveBeenLastCalledWith(
			expect.objectContaining({ pageNum: 3, keyword: 'Vue', categoryId: 2, tagIds: [1] }),
			expect.any(Object),
		);
		wrapper.unmount();
	});

	it('reloads only when returning to a different set of filters', async () => {
		const { router, wrapper } = await mountList();
		await router.push('/articles/1');
		await flushPromises();
		expect(listArticles).toHaveBeenCalledTimes(1);
		vi.mocked(listArticles).mockResolvedValueOnce(page(8));

		await router.push('/articles');
		await flushPromises();

		expect(wrapper.findAll('.test-article').map((row) => row.text())).toEqual(['文章 8']);
		expect(listArticles).toHaveBeenLastCalledWith(
			expect.objectContaining({
				pageNum: 1,
				keyword: undefined,
				categoryId: undefined,
				tagIds: [],
			}),
			expect.any(Object),
		);
		expect((wrapper.get('input').element as HTMLInputElement).value).toBe('');
		wrapper.unmount();
	});

	it('mounts the load-more trigger only after restoration and does not cache details', async () => {
		const { router, wrapper } = await mountList('/articles');
		prepareArticleListScroll(true);
		await flushPromises();
		expect(articleListScrollPending.value).toBe(true);
		expect(wrapper.findComponent(AppLoadMoreTrigger).exists()).toBe(false);
		prepareArticleListScroll(false);
		await flushPromises();
		expect(wrapper.findComponent(AppLoadMoreTrigger).exists()).toBe(true);
		await router.push('/articles/1');
		await flushPromises();
		router.back();
		await flushPromises();
		await router.push('/articles/1');
		await flushPromises();

		expect(detailMounted).toHaveBeenCalledTimes(2);
		expect(listArticles).toHaveBeenCalledTimes(1);
		wrapper.unmount();
	});

	it('does not mark a stale request ready after navigating away', async () => {
		let resolveList!: (value: PublicArticlePageData) => void;
		vi.mocked(listArticles).mockReturnValueOnce(
			new Promise((resolve) => {
				resolveList = resolve;
			}),
		);
		const { router, wrapper } = await mountList('/articles');
		await router.push('/articles/1');
		resolveList(page(4));
		await flushPromises();

		expect(wrapper.find('.test-detail').exists()).toBe(true);
		expect(wrapper.findComponent(AppLoadMoreTrigger).exists()).toBe(false);
		router.back();
		await flushPromises();
		expect(wrapper.get('.test-article').text()).toBe('文章 4');
		expect(listArticles).toHaveBeenCalledTimes(1);
		wrapper.unmount();
	});

	it('restores on insertion of the cached list without waiting for its fade-in to finish', async () => {
		const style = document.createElement('style');
		style.dataset.articleListTransitionTest = 'true';
		style.textContent =
			'.fade-enter-active { transition-property: opacity; transition-duration: 10s; transition-delay: 0s; }';
		document.head.append(style);
		vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
		const { router, wrapper } = await mountList('/articles', true);
		await vi.waitFor(() => expect(articleListScrollPending.value).toBe(false));
		const listElement = wrapper.get('.article-list-page').element;
		await router.push('/articles/1');
		await vi.waitFor(() => expect(wrapper.find('.test-detail').exists()).toBe(true));
		vi.mocked(window.scrollTo).mockClear();

		router.back();
		await flushPromises();
		expect(window.scrollTo).not.toHaveBeenCalled();
		await vi.waitFor(() => expect(articleListScrollPending.value).toBe(false));

		expect(wrapper.get('.article-list-page').element).toBe(listElement);
		expect(wrapper.get('.article-list-page').classes()).toContain('fade-enter-active');
		expect(window.scrollTo).toHaveBeenCalledExactlyOnceWith({
			left: 0,
			top: 1500,
			behavior: 'instant',
		});
		expect(listArticles).toHaveBeenCalledTimes(1);
		await wrapper.get('.article-list-page').trigger('transitionend');
		style.remove();
		wrapper.unmount();
	});

	it('retries an initial list failure when entering the cached page again', async () => {
		vi.mocked(listArticles).mockRejectedValueOnce(new Error('test network unavailable'));
		const { router, wrapper } = await mountList('/articles');
		await router.push('/articles/1');
		await flushPromises();
		vi.mocked(listArticles).mockResolvedValueOnce(page(5));

		router.back();
		await flushPromises();

		expect(wrapper.get('.test-article').text()).toBe('文章 5');
		expect(listArticles).toHaveBeenCalledTimes(2);
		wrapper.unmount();
	});
});
