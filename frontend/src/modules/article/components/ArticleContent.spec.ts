import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import confetti from 'canvas-confetti';
import type { PublicArticleDetailData } from '../types/article';
import ArticleContent from './ArticleContent.vue';
import ArticleLikeButton from './ArticleLikeButton.vue';
import AppFixedButton from '@/components/AppFixedButton.vue';
import { useArticleCatalog } from '../composables/useArticleCatalog';

const refreshCatalog = vi.hoisted(() => vi.fn());

// jsdom 不提供 Canvas 2D 上下文，测试彩带调用而不执行第三方动画帧。
vi.mock('canvas-confetti', () => ({ default: vi.fn() }));

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

const PublicUserProfilePopoverStub = defineComponent({
	props: {
		userId: Number,
	},
	template: '<div class="public-profile-popover-stub"><slot /></div>',
});

const AppUserAvatarStub = defineComponent({
	props: {
		avatarUrl: String,
		name: String,
		userId: Number,
		size: Number,
	},
	template: '<span class="user-avatar-stub" />',
});

function article(): PublicArticleDetailData {
	return {
		seo: {
			title: '测试文章 - 青禾边',
			description: '文章摘要',
			canonicalUrl: 'https://blog.example.com/articles/40001',
			imageUrl: null,
			type: 'article',
			publishedAt: null,
			updatedAt: null,
		},
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
		liked: false,
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
			role: 'ADMIN',
			bio: '',
		},
	};
}

describe('ArticleContent', () => {
	beforeEach(() => vi.clearAllMocks());

	it('keeps the left actions aligned and closes the catalog before requesting comments', async () => {
		const currentArticle = { ...article(), allowComment: false };
		const wrapper = mount(ArticleContent, {
			props: { article: currentArticle, isLoading: false },
			global: {
				stubs: {
					RouterLink: RouterLinkStub,
					ArticleCatalogSidebar: ArticleCatalogSidebarStub,
					AppUserAvatar: AppUserAvatarStub,
					PublicUserProfilePopover: PublicUserProfilePopoverStub,
				},
			},
		});
		const commentButton = wrapper
			.findAllComponents(AppFixedButton)
			.find((button) => button.classes().includes('article-comment-button'))!;
		const catalog = vi.mocked(useArticleCatalog).mock.results.at(-1)!.value;
		const likeButton = wrapper.getComponent(ArticleLikeButton);

		expect(likeButton.props('offsetY')).toBe(168);
		expect(commentButton.props()).toMatchObject({ placement: 'bottom-left', offsetY: 100 });
		expect(commentButton.attributes('aria-controls')).toBe('article-comments');
		expect(commentButton.attributes('aria-label')).toBe('跳转到评论区');
		await wrapper.get('.catalog-open').trigger('click');
		expect(commentButton.classes()).toContain('article-comment-button-expanded');
		expect(likeButton.get('button').classes()).toContain('article-like-button-expanded');

		await commentButton.trigger('click');
		expect(wrapper.emitted('jump-to-comments')).toHaveLength(1);
		expect(commentButton.classes()).not.toContain('article-comment-button-expanded');
		expect(likeButton.props('expanded')).toBe(false);
		expect(wrapper.getComponent(ArticleCatalogSidebarStub).props('expanded')).toBe(false);
		expect(wrapper.emitted('toggle-like')).toBeUndefined();

		catalog.catalogList.value = [];
		await wrapper.vm.$nextTick();
		expect(likeButton.props('offsetY')).toBe(100);
		expect(commentButton.props('offsetY')).toBe(32);
		await commentButton.trigger('click');
		expect(wrapper.emitted('jump-to-comments')).toHaveLength(2);

		await wrapper.setProps({ article: null });
		expect(wrapper.find('.article-comment-button').exists()).toBe(false);
		expect(wrapper.findComponent(ArticleLikeButton).exists()).toBe(false);
		wrapper.unmount();
	});

	it('forwards the like button click without changing article state locally', async () => {
		const currentArticle = article();
		const wrapper = mount(ArticleContent, {
			props: { article: currentArticle, isLoading: false },
			global: {
				stubs: {
					RouterLink: RouterLinkStub,
					ArticleCatalogSidebar: ArticleCatalogSidebarStub,
					AppImage: true,
					AppTagCapsule: true,
					AppUserAvatar: AppUserAvatarStub,
					PublicUserProfilePopover: PublicUserProfilePopoverStub,
					ILucideThumbsUp: true,
				},
			},
		});

		await wrapper.get('.article-like-button').trigger('click');

		expect(wrapper.emitted('toggle-like')).toHaveLength(1);
		expect(wrapper.getComponent(ArticleLikeButton).props('isLiked')).toBe(false);
		expect(currentArticle.likeCount).toBe(1);
		expect(confetti).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ angle: 45 }));

		const statsButton = wrapper.get('.article-statis-like');
		expect(statsButton.attributes('aria-pressed')).toBe('false');
		await statsButton.trigger('click', { clientX: 120, clientY: 80 });
		expect(wrapper.emitted('toggle-like')).toHaveLength(2);
		expect(confetti).toHaveBeenCalledTimes(2);
		expect(confetti).toHaveBeenLastCalledWith({
			particleCount: 80,
			angle: 90,
			spread: 90,
			origin: { x: 120 / window.innerWidth, y: 80 / window.innerHeight },
		});
		expect(currentArticle.likeCount).toBe(1);
		await wrapper.setProps({ article: { ...currentArticle, liked: true, likeCount: 2 } });
		expect(statsButton.text()).toBe('2');
		expect(statsButton.attributes('aria-pressed')).toBe('true');
		expect(statsButton.attributes('aria-label')).toBe('取消文章点赞');
		expect(statsButton.classes()).toContain('article-statis-like--liked');
		expect(wrapper.getComponent(ArticleLikeButton).props('isLiked')).toBe(true);
		await wrapper.setProps({ updatingLike: true });
		expect(statsButton.attributes('disabled')).toBeDefined();
		await statsButton.trigger('click');
		expect(wrapper.emitted('toggle-like')).toHaveLength(2);
		expect(confetti).toHaveBeenCalledTimes(2);
		wrapper.unmount();
	});

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
					AppUserAvatar: AppUserAvatarStub,
					PublicUserProfilePopover: PublicUserProfilePopoverStub,
				},
			},
		});

		await wrapper.get('.catalog-open').trigger('click');
		expect(wrapper.get('.article-content-wrapper').classes()).toContain(
			'article-content-wrapper-squeeze-to-the-right',
		);
		await wrapper.get('.article-statis-like').trigger('click');
		expect(wrapper.get('.article-content-wrapper').classes()).toContain(
			'article-content-wrapper-squeeze-to-the-right',
		);

		await wrapper.get('.article-title').trigger('click');
		expect(wrapper.get('.article-content-wrapper').classes()).not.toContain(
			'article-content-wrapper-squeeze-to-the-right',
		);

		expect(wrapper.getComponent(PublicUserProfilePopoverStub).props('userId')).toBe(10001);
		expect(wrapper.getComponent(AppUserAvatarStub).props()).toMatchObject({
			avatarUrl: '',
			name: '管理员',
			userId: 10001,
			size: 30,
		});
		expect(wrapper.get('.public-profile-popover-stub').text()).toContain('管理员');
		wrapper.unmount();
	});

	it('removes only image title tooltips after rendering and updating article HTML', async () => {
		const currentArticle = article();
		currentArticle.contentHtml =
			'<p><img src="https://img.example.com/test.png" title="图片标题" alt="图片说明" width="320" height="180"></p><a href="https://example.com" title="链接说明">链接</a>';
		const wrapper = mount(ArticleContent, {
			props: { article: currentArticle, isLoading: false },
			global: {
				stubs: {
					RouterLink: RouterLinkStub,
					ArticleCatalogSidebar: ArticleCatalogSidebarStub,
					AppImage: true,
					AppTagCapsule: true,
					AppUserAvatar: AppUserAvatarStub,
					PublicUserProfilePopover: PublicUserProfilePopoverStub,
					ILucideThumbsUp: true,
				},
			},
		});
		await wrapper.vm.$nextTick();
		const image = wrapper.get('.article-markdown img');
		expect(image.attributes('title')).toBeUndefined();
		expect(image.attributes('alt')).toBe('图片说明');
		expect(image.attributes('width')).toBe('320');
		expect(image.attributes('height')).toBe('180');
		expect(wrapper.get('.article-markdown a').attributes('title')).toBe('链接说明');
		expect(currentArticle.contentHtml).toContain('title="图片标题"');
		await wrapper.setProps({
			article: {
				...currentArticle,
				contentHtml:
					'<img src="https://img.example.com/new.png" title="新图片标题" alt="新图片">',
			},
		});
		await wrapper.vm.$nextTick();
		expect(wrapper.get('.article-markdown img').attributes('title')).toBeUndefined();
		expect(wrapper.get('.article-markdown img').attributes('alt')).toBe('新图片');
		wrapper.unmount();
	});

	it('renders TeX formulas after article HTML updates and leaves code blocks untouched', async () => {
		const currentArticle = article();
		currentArticle.contentHtml =
			'<p>Inline $2^3$ and \\(x^2\\)</p><p>$$\\frac{1}{2}$$</p><p>Invalid $\\notARealCommand$</p><pre><code>$2^3$</code></pre>';
		const wrapper = mount(ArticleContent, {
			props: { article: currentArticle, isLoading: false },
			global: {
				stubs: {
					RouterLink: RouterLinkStub,
					ArticleCatalogSidebar: ArticleCatalogSidebarStub,
					AppImage: true,
					AppTagCapsule: true,
					AppUserAvatar: AppUserAvatarStub,
					PublicUserProfilePopover: PublicUserProfilePopoverStub,
					ILucideThumbsUp: true,
				},
			},
		});

		await wrapper.vm.$nextTick();
		await vi.dynamicImportSettled();
		await flushPromises();
		expect(wrapper.findAll('.article-markdown .katex')).toHaveLength(4);
		expect(wrapper.findAll('.article-markdown .katex-display')).toHaveLength(1);
		expect(wrapper.get('.article-markdown pre code').text()).toBe('$2^3$');
		expect(wrapper.find('.article-markdown pre .katex').exists()).toBe(false);
		expect(wrapper.get('.article-markdown p:nth-of-type(3)').text()).toContain(
			'notARealCommand',
		);

		await wrapper.setProps({
			article: { ...currentArticle, contentHtml: '<p>Updated $x_1$</p>' },
		});
		await wrapper.vm.$nextTick();
		await vi.dynamicImportSettled();
		await flushPromises();
		expect(wrapper.findAll('.article-markdown .katex')).toHaveLength(1);
		expect(wrapper.get('.article-markdown .katex').text()).toContain('x');
		wrapper.unmount();
	});
});
