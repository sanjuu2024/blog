import { mount } from '@vue/test-utils';
import { ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CATEGORY_LEVEL } from '../constants/category';
import type { PublicCategoryItem } from '../types/category';
import { useEnabledCategoryList } from '../composables/useEnabledCategoryList';
import CategoryPage from './CategoryPage.vue';

const mediaQueryState = vi.hoisted(() => ({ isMobile: false }));
const route = vi.hoisted(() => ({ params: { categoryId: '21001' } }));

vi.mock('@vueuse/core', async (importOriginal) => {
	const { computed } = await import('vue');
	return {
		...(await importOriginal<typeof import('@vueuse/core')>()),
		useMediaQuery: vi.fn(() => computed(() => mediaQueryState.isMobile)),
	};
});

vi.mock('vue-router', async (importOriginal) => ({
	...(await importOriginal<typeof import('vue-router')>()),
	useRoute: vi.fn(() => route),
}));

vi.mock('../composables/useEnabledCategoryList', () => ({
	useEnabledCategoryList: vi.fn(),
}));

function categoryList(): PublicCategoryItem[] {
	return [
		{
			id: 20001,
			parentId: null,
			name: '技术',
			level: CATEGORY_LEVEL.FIRST,
			description: '技术文章',
			sortNo: 1,
			articleCount: 8,
			children: [
				{
					id: 21001,
					parentId: 20001,
					name: 'Java',
					level: CATEGORY_LEVEL.SECOND,
					description: 'Java 文章',
					sortNo: 1,
					articleCount: 5,
					children: [],
				},
			],
		},
	];
}

function mountPage() {
	return mount(CategoryPage, {
		global: {
			stubs: {
				RouterLink: { template: '<a><slot /></a>' },
				RouterView: { template: '<div><slot /></div>' },
				PrimaryCategoryList: {
					props: ['category'],
					template: '<div class="primary-category">{{ category.name }}</div>',
				},
				ILucideLayers: true,
				ILucideX: true,
			},
		},
	});
}

describe('CategoryPage', () => {
	beforeEach(() => {
		mediaQueryState.isMobile = false;
		route.params.categoryId = '21001';
		vi.mocked(useEnabledCategoryList).mockReturnValue({
			enabledCategoryList: ref(categoryList()),
			loading: ref(false),
			loadFailed: ref(false),
			loaded: ref(true),
			getEnabledCategoryList: vi.fn(),
		});
	});

	it('keeps the category list visible in the desktop layout', () => {
		const wrapper = mountPage();

		expect(wrapper.get('.category-list-wrapper').isVisible()).toBe(true);
		expect(wrapper.get('.category-page__mobile-header').isVisible()).toBe(false);
	});

	it('opens and closes the category list in the mobile layout', async () => {
		mediaQueryState.isMobile = true;
		const wrapper = mountPage();
		const categoryListWrapper = wrapper.get('.category-list-wrapper');
		const mobileHeader = wrapper.get('.category-page__mobile-header');

		expect(mobileHeader.text()).toContain('Java');
		expect(mobileHeader.attributes('style') ?? '').not.toContain('display: none');
		expect(categoryListWrapper.attributes('style')).toContain('display: none');

		await wrapper.get('.category-page__mobile-header-button').trigger('click');
		expect(wrapper.get('.category-page__mobile-header').attributes('style')).toContain(
			'display: none',
		);
		expect(wrapper.get('.category-list-wrapper').attributes('style') ?? '').not.toContain(
			'display: none',
		);

		await wrapper.get('.category-list-wrapper__close-button').trigger('click');
		expect(
			wrapper.get('.category-page__mobile-header').attributes('style') ?? '',
		).not.toContain('display: none');
		expect(wrapper.get('.category-list-wrapper').attributes('style')).toContain(
			'display: none',
		);
	});
});
