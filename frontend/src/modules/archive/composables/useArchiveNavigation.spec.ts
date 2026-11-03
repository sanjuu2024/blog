import { flushPromises, mount } from '@vue/test-utils';
import { createMemoryHistory, createRouter, RouterView } from 'vue-router';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getEnabledCategories } from '@/modules/category/api/categoryApi';
import { getEnabledTags } from '@/modules/tag/api/tagApi';
import { getArchiveYears } from '../api/archiveApi';
import { useArchiveNavigation } from './useArchiveNavigation';

vi.mock('@/modules/category/api/categoryApi', () => ({ getEnabledCategories: vi.fn() }));
vi.mock('@/modules/tag/api/tagApi', () => ({ getEnabledTags: vi.fn() }));
vi.mock('../api/archiveApi', () => ({ getArchiveYears: vi.fn() }));

const Harness = defineComponent({
	setup: useArchiveNavigation,
	template:
		'<div><span class="mode">{{ mode }}</span><span class="title">{{ title }}</span><span class="filter">{{ query?.year ?? query?.categoryId ?? query?.tagId ?? "all" }}</span></div>',
});

async function mountArchive(path: string) {
	const router = createRouter({
		history: createMemoryHistory(),
		routes: [{ path: '/archives', name: 'Archive', component: Harness }],
	});
	const wrapper = mount(RouterView, { global: { plugins: [router] } });
	await router.push(path);
	await router.isReady();
	await flushPromises();
	return { router, wrapper };
}

describe('useArchiveNavigation', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(getArchiveYears).mockResolvedValue([
			{ year: 2026, articleCount: 4 },
			{ year: 2025, articleCount: 8 },
		]);
		vi.mocked(getEnabledCategories).mockResolvedValue([
			{
				id: 11,
				parentId: null,
				name: '技术',
				level: 1,
				description: '',
				sortNo: 1,
				articleCount: 1,
				children: [],
			},
		]);
		vi.mocked(getEnabledTags).mockResolvedValue([]);
	});

	it('defaults to the latest populated year and canonicalizes an unavailable year', async () => {
		const { router, wrapper } = await mountArchive('/archives?mode=time&year=2030');

		expect(wrapper.get('.mode').text()).toBe('time');
		expect(wrapper.get('.title').text()).toBe('2026 年');
		expect(wrapper.get('.filter').text()).toBe('2026');
		expect(router.currentRoute.value.fullPath).toBe('/archives?mode=time&year=2026');
		wrapper.unmount();
	});

	it('drops an unavailable category and falls back to all articles', async () => {
		const { router, wrapper } = await mountArchive('/archives?mode=category&categoryId=999');

		expect(wrapper.get('.mode').text()).toBe('category');
		expect(wrapper.get('.title').text()).toBe('全部文章');
		expect(wrapper.get('.filter').text()).toBe('all');
		expect(router.currentRoute.value.fullPath).toBe('/archives?mode=category');
		wrapper.unmount();
	});
});
