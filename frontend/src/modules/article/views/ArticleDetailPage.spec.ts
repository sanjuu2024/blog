import { flushPromises, shallowMount } from '@vue/test-utils';
import { defineComponent, reactive, ref } from 'vue';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import ArticleDetailPage from './ArticleDetailPage.vue';

const locate = vi.hoisted(() => vi.fn());
const stop = vi.hoisted(() => vi.fn());
const route = reactive({
	params: { articleId: '34' },
	fullPath: '/articles/34#article-comments',
	hash: '#article-comments',
	query: {} as { replyId?: string },
});
const article = ref<Record<string, unknown> | null>(null);

vi.mock('vue-router', async (importOriginal) => ({
	...(await importOriginal<typeof import('vue-router')>()),
	useRoute: () => route,
}));
vi.mock('../composables/useArticleDetail', () => ({
	useArticleDetail: () => ({
		article,
		isLoading: ref(false),
		errorMessage: ref(''),
		updatingLike: ref(false),
		getArticleDetail: vi.fn(),
		toggleArticleLike: vi.fn(),
	}),
}));
vi.mock('../composables/useArticleCommentAnchor', () => ({
	useArticleCommentAnchor: () => ({ locate, stop }),
}));

const CommentSectionStub = defineComponent({
	name: 'PublicCommentSection',
	emits: ['reply-located'],
	template: '<section id="article-comments" />',
});

function mountPage() {
	return shallowMount(ArticleDetailPage, {
		global: { stubs: { PublicCommentSection: CommentSectionStub, AppBacktop: true } },
	});
}

describe('ArticleDetailPage comment navigation', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		article.value = null;
		route.fullPath = '/articles/34#article-comments';
		route.query = {};
	});
	afterEach(() => (article.value = null));

	it('locates the comment section only after article data renders', async () => {
		const wrapper = mountPage();
		await flushPromises();
		expect(locate).not.toHaveBeenCalled();
		article.value = { id: 34 };
		await flushPromises();
		expect(locate).toHaveBeenCalledExactlyOnceWith(wrapper.get('#article-comments').element);
		wrapper.unmount();
	});

	it('lets the notification target take precedence over the section anchor', async () => {
		route.query = { replyId: '51' };
		route.fullPath = '/articles/34?replyId=51#article-comments';
		const wrapper = mountPage();
		article.value = { id: 34 };
		await flushPromises();
		expect(locate).not.toHaveBeenCalled();
		const reply = document.createElement('div');
		wrapper.getComponent(CommentSectionStub).vm.$emit('reply-located', reply);
		expect(locate).toHaveBeenCalledExactlyOnceWith(reply);
		wrapper.unmount();
	});

	it('stops correcting the old target when navigating to another article', async () => {
		const wrapper = mountPage();
		article.value = { id: 34 };
		await flushPromises();
		stop.mockClear();
		route.params.articleId = '35';
		route.fullPath = '/articles/35';
		route.hash = '';
		await flushPromises();
		expect(stop).toHaveBeenCalled();
		expect(locate).toHaveBeenCalledTimes(1);
		wrapper.unmount();
		route.params.articleId = '34';
		route.hash = '#article-comments';
	});
});
