import { flushPromises, mount } from '@vue/test-utils';
import { createMemoryHistory, createRouter, RouterView } from 'vue-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { listArticles } from '@/modules/article/api/articleApi';
import { getEnabledCategories } from '@/modules/category/api/categoryApi';
import { getEnabledTags } from '@/modules/tag/api/tagApi';
import { getArchiveArticles, getArchiveYears } from '@/modules/archive/api/archiveApi';
import ArchivePage from '@/modules/archive/views/ArchivePage.vue';
import HomePage from './HomePage.vue';

vi.mock('@/modules/article/api/articleApi', () => ({ listArticles: vi.fn() }));
vi.mock('@/modules/category/api/categoryApi', () => ({ getEnabledCategories: vi.fn() }));
vi.mock('@/modules/tag/api/tagApi', () => ({ getEnabledTags: vi.fn() }));
vi.mock('@/modules/archive/api/archiveApi', () => ({
	getArchiveYears: vi.fn(),
	getArchiveArticles: vi.fn(),
}));

// 复用词云的插槽契约，避免在导航测试中执行 Canvas 排版和动画。
vi.mock('vuewordcloud', async () => {
	const { defineComponent, h } = await import('vue');
	return {
		default: defineComponent({
			props: {
				words: {
					type: Array<{ id: number; text: string; weight: number }>,
					required: true,
				},
			},
			setup(props, { slots }) {
				return () =>
					h(
						'div',
						props.words.map((word) => slots.default?.({ text: word.text, word })),
					);
			},
		}),
	};
});

describe('homepage tag cloud navigation', () => {
	let wrapper: ReturnType<typeof mount>;
	const tags = [
		{ id: 30001, name: 'Vue', articleCount: 7 },
		{ id: 30002, name: 'C++ / 入门', articleCount: 2 },
	];

	beforeEach(() => {
		vi.clearAllMocks();
		const emptyPage = {
			total: 0,
			totalPages: 0,
			pageNum: 1,
			pageSize: 10,
			hasNext: false,
			records: [],
		};
		vi.mocked(listArticles).mockResolvedValue(emptyPage);
		vi.mocked(getArchiveArticles).mockResolvedValue(emptyPage);
		vi.mocked(getEnabledCategories).mockResolvedValue([]);
		vi.mocked(getEnabledTags).mockResolvedValue(tags);
		vi.mocked(getArchiveYears).mockResolvedValue([]);
	});

	afterEach(() => wrapper?.unmount());

	async function start() {
		const router = createRouter({
			history: createMemoryHistory(),
			routes: [
				{ path: '/', name: 'Home', component: HomePage },
				{ path: '/archives', name: 'Archive', component: ArchivePage },
			],
		});
		await router.push('/');
		wrapper = mount(RouterView, {
			global: {
				plugins: [router],
				stubs: {
					AppImage: true,
					TopArticleCard: true,
					LatestArticleListItem: true,
					CategoryCard: true,
					AppLoadMoreTrigger: true,
				},
			},
		});
		await flushPromises();
		return router;
	}

	it('links words by tag ID and opens the matching archive filter', async () => {
		const router = await start();
		const links = wrapper.findAll('.home-tags__link');
		expect(links.map((link) => link.text())).toEqual(tags.map((tag) => tag.name));
		expect(links.map((link) => link.attributes('href'))).toEqual([
			'/archives?mode=tag&tagId=30001',
			'/archives?mode=tag&tagId=30002',
		]);
		expect(links.every((link) => link.element.tagName === 'A')).toBe(true);

		await links[1]!.trigger('click');
		await flushPromises();

		expect(router.currentRoute.value.query).toEqual({ mode: 'tag', tagId: '30002' });
		expect(wrapper.get('.archive-tag-capsule.is-active').text()).toBe('#C++ / 入门 (2)');
		expect(getArchiveArticles).toHaveBeenLastCalledWith(
			{
				tagId: 30002,
				timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
				pageNum: 1,
				pageSize: 10,
			},
			expect.any(AbortSignal),
		);
	});

	it('keeps empty and failed tag lists free of navigation links', async () => {
		vi.mocked(getEnabledTags).mockResolvedValueOnce([]);
		await start();
		expect(wrapper.get('.home-tags').text()).toContain('暂无标签');
		expect(wrapper.find('.home-tags__link').exists()).toBe(false);
		wrapper.unmount();

		vi.mocked(getEnabledTags).mockRejectedValueOnce(new Error('load failed'));
		await start();
		expect(wrapper.get('.home-tags').text()).toContain('标签云加载失败');
		expect(wrapper.find('.home-tags__link').exists()).toBe(false);
	});
});
