import { flushPromises, mount } from '@vue/test-utils';
import { createMemoryHistory, createRouter, RouterView } from 'vue-router';
import { computed } from 'vue';
import { useMediaQuery } from '@vueuse/core';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { getEnabledCategories } from '@/modules/category/api/categoryApi';
import { getEnabledTags } from '@/modules/tag/api/tagApi';
import { getArchiveArticles, getArchiveYears } from '../api/archiveApi';
import ArchivePage from './ArchivePage.vue';

vi.mock('@/modules/category/api/categoryApi', () => ({ getEnabledCategories: vi.fn() }));
vi.mock('@/modules/tag/api/tagApi', () => ({ getEnabledTags: vi.fn() }));
vi.mock('../api/archiveApi', () => ({
	getArchiveYears: vi.fn(),
	getArchiveArticles: vi.fn(),
}));

vi.mock('@vueuse/core', async (importOriginal) => ({
	...(await importOriginal<typeof import('@vueuse/core')>()),
	useMediaQuery: vi.fn(),
}));

async function mountArchivePage(path: string) {
	const router = createRouter({
		history: createMemoryHistory(),
		routes: [{ path: '/archives', name: 'Archive', component: ArchivePage }],
	});
	await router.push(path);
	const wrapper = mount(RouterView, {
		attachTo: document.body,
		global: {
			plugins: [router],
			stubs: { AppLoadMoreTrigger: true, AppLoading: true },
		},
	});
	await flushPromises();
	return { router, wrapper };
}

describe('ArchivePage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(useMediaQuery).mockReturnValue(computed(() => false));
		const browserOptions = new Intl.DateTimeFormat().resolvedOptions();
		vi.spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions').mockReturnValue({
			...browserOptions,
			timeZone: 'America/Los_Angeles',
		});
		vi.mocked(getArchiveYears).mockResolvedValue([{ year: 2026, articleCount: 1 }]);
		vi.mocked(getEnabledCategories).mockResolvedValue([]);
		vi.mocked(getEnabledTags).mockResolvedValue([
			{ id: 1, name: 'Vue', articleCount: 7 },
			{ id: 2, name: '测试一个最长的标签'.repeat(8), articleCount: 0 },
		]);
		vi.mocked(getArchiveArticles).mockResolvedValue({
			total: 0,
			totalPages: 0,
			pageNum: 1,
			pageSize: 10,
			hasNext: false,
			records: [],
		});
	});

	afterEach(() => {
		vi.restoreAllMocks();
		document.body.innerHTML = '';
	});

	it('switches modes through radio buttons and restores the selected tag from browser history', async () => {
		const { router, wrapper } = await mountArchivePage('/archives?mode=time&year=2026');
		expect(getArchiveYears).toHaveBeenCalledWith('America/Los_Angeles');
		expect(getArchiveArticles).toHaveBeenCalledWith(
			{ year: 2026, timeZone: 'America/Los_Angeles', pageNum: 1, pageSize: 10 },
			expect.any(AbortSignal),
		);

		await wrapper.get('input[value="tag"]').setValue(true);
		await flushPromises();
		expect(router.currentRoute.value.query).toEqual({ mode: 'tag' });

		const tags = wrapper.findAll('.archive-tag-capsule');
		expect(tags[0].text()).toBe('#Vue (7)');
		expect(tags[1].text()).toBe(`#${'测试一个最长的标签'.repeat(8)} (0)`);
		await tags[1].trigger('click');
		await flushPromises();
		expect(router.currentRoute.value.query).toEqual({ mode: 'tag', tagId: '2' });
		expect(wrapper.get('.archive-tag-capsule.is-active').attributes('aria-current')).toBe(
			'page',
		);

		await wrapper.get('input[value="category"]').setValue(true);
		await flushPromises();
		expect(router.currentRoute.value.query).toEqual({ mode: 'category' });

		router.back();
		await flushPromises();
		expect(router.currentRoute.value.query).toEqual({ mode: 'tag', tagId: '2' });
		expect(wrapper.get<HTMLInputElement>('input[value="tag"]').element.checked).toBe(true);
		expect(wrapper.get('.archive-tag-capsule.is-active').attributes('href')).toBe(
			'/archives?mode=tag&tagId=2',
		);
		for (const [params] of vi.mocked(getArchiveArticles).mock.calls) {
			expect(params.timeZone).toBe('America/Los_Angeles');
		}
		wrapper.unmount();
	});

	it.each([
		['/archives?mode=time&year=2026', '按时间', '2026 年'],
		['/archives?mode=category&categoryId=22', '按分类', '技术 / Java'],
		['/archives?mode=tag&tagId=1', '按标签', '#Vue'],
	])('shows the mobile mode entry and selected range for %s', async (path, modeLabel, title) => {
		vi.mocked(useMediaQuery).mockReturnValue(computed(() => true));
		vi.mocked(getEnabledCategories).mockResolvedValue([
			{
				id: 11,
				parentId: null,
				name: '技术',
				level: 1,
				description: '',
				sortNo: 1,
				articleCount: 1,
				children: [
					{
						id: 22,
						parentId: 11,
						name: 'Java',
						level: 2,
						description: '',
						sortNo: 1,
						articleCount: 1,
						children: [],
					},
				],
			},
		]);
		const { wrapper } = await mountArchivePage(path);
		const navigation = wrapper.get('.category-list-wrapper');
		const entry = wrapper.get('.archive-page__mobile-header-button');

		expect(entry.text()).toBe(modeLabel);
		expect(wrapper.get('.archive-heading h1').text()).toBe(title);
		expect(navigation.isVisible()).toBe(false);
		expect(wrapper.get('.archive-page__mobile-header').isVisible()).toBe(true);

		await entry.trigger('click');
		expect(navigation.isVisible()).toBe(true);
		expect(wrapper.get('.archive-page__mobile-header').isVisible()).toBe(false);
		await navigation.get('.category-list-wrapper__close-button').trigger('click');
		expect(navigation.isVisible()).toBe(false);

		await entry.trigger('click');
		const selectedLink = navigation.get('a.is-active');
		expect(selectedLink.attributes('href')).toBe(path);
		await selectedLink.trigger('click');
		await flushPromises();
		expect(navigation.isVisible()).toBe(false);
		expect(entry.text()).toBe(modeLabel);
		expect(wrapper.get('.archive-heading h1').text()).toBe(title);
		wrapper.unmount();
	});
});
