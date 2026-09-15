import { mount, RouterLinkStub } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import type { PublicArticleDetailData } from '../types/article';
import ArticleContent from './ArticleContent.vue';

const refreshCatalog = vi.hoisted(() => vi.fn());

vi.mock('../composables/useArticleCatalog', async () => {
	const { ref } = await import('vue');
	return {
		useArticleCatalog: vi.fn(() => ({
			activeCatalogId: ref('section-1'),
			catalogList: ref([{ id: 'section-1', text: '第一节', level: 2 }]),
			refreshCatalog,
			scrollToHeading: vi.fn(),
		})),
	};
});

vi.mock('@/utils/prism', () => ({
	highlightCodeUnder: vi.fn(),
}));

vi.mock('@/utils/getRouteIcon', async () => {
	const { defineComponent } = await import('vue');
	return {
		default: vi.fn(() => defineComponent({ template: '<span />' })),
	};
});

const ArticleCatalogSidebarStub = defineComponent({
	props: {
		expanded: Boolean,
	},
	emits: ['update:expanded'],
	template:
		'<button class="catalog-open" @click.stop="$emit(\'update:expanded\', true)">打开目录</button>',
});

function article(): PublicArticleDetailData {
	return {
		id: 40001,
		title: '测试文章',
		summary: '文章摘要',
		contentHtml: '<h2>第一节</h2><p>正文</p>',
		coverUrl: '',
		isTop: false,
		allowComment: true,
		viewCount: 10,
		commentCount: 2,
		likeCount: 1,
		publishedAt: '2026-09-01T10:00:00+08:00',
		updatedAt: '2026-09-01T10:00:00+08:00',
		category: {
			id: 21001,
			name: 'Java',
			level: 2,
			parent: { id: 20001, name: '技术' },
		},
		tags: [],
		author: {
			id: 10001,
			username: 'admin',
			nickname: '管理员',
			avatarUrl: '',
			bio: '',
		},
	};
}

describe('ArticleContent', () => {
	it('closes the catalog when the article content area is clicked', async () => {
		const wrapper = mount(ArticleContent, {
			props: {
				article: article(),
				isLoading: false,
			},
			global: {
				stubs: {
					RouterLink: RouterLinkStub,
					ArticleCatalogSidebar: ArticleCatalogSidebarStub,
					AppImage: true,
					AppTagCapsule: true,
				},
			},
		});

		await wrapper.get('.catalog-open').trigger('click');
		expect(wrapper.get('.article-content-wrapper').classes()).toContain(
			'article-content-wrapper-squeeze-to-the-right',
		);

		await wrapper.get('.article-title').trigger('click');
		expect(wrapper.get('.article-content-wrapper').classes()).not.toContain(
			'article-content-wrapper-squeeze-to-the-right',
		);
	});
});
