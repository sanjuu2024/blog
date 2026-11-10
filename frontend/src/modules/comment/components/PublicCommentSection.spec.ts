import { flushPromises, mount } from '@vue/test-utils';
import { nextTick, reactive, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { useUserStore } from '@/stores/userStore';
import PublicCommentSection from './PublicCommentSection.vue';
import { usePublicCommentList } from '../composables/usePublicCommentList';
import { COMMENT_STATUS, type CommentReplyState, type PublicCommentItem } from '../types/comment';

vi.mock('vue-router', async (importOriginal) => ({
	...(await importOriginal<typeof import('vue-router')>()),
	useRoute: vi.fn(),
	useRouter: vi.fn(),
}));

vi.mock('@/stores/authStore', () => ({
	useAuthStore: vi.fn(),
}));

vi.mock('@/stores/userStore', () => ({
	useUserStore: vi.fn(),
}));

vi.mock('../composables/usePublicCommentList', () => ({
	usePublicCommentList: vi.fn(),
}));

const commentList = ref<PublicCommentItem[]>([]);
const replyState = reactive<CommentReplyState>({
	records: [],
	nextCursor: null,
	hasNext: false,
	loading: false,
	loaded: false,
	expanded: false,
});
const getReplyList = vi.fn(async () => {
	replyState.loaded = true;
	replyState.expanded = true;
});
const collapseReplies = vi.fn(() => {
	replyState.expanded = false;
});

const stubs = {
	AppLoadMoreTrigger: true,
	AppUserAvatar: true,
	ElAlert: true,
	ElButton: { template: '<button><slot /></button>' },
	ElInput: true,
	ElTag: true,
	ILucideChevronRight: true,
	ILucideChevronDown: true,
	ILucideLoader: true,
	ILucideMessageCircleReply: true,
	ILucideTrash2: true,
	ILucideThumbsUp: true,
	PublicCommentContent: { template: '<div><slot name="reason" /><slot name="actions" /></div>' },
	PublicCommentReplyEditor: true,
	PublicUserProfilePopover: { template: '<div><slot /></div>' },
};

function comment(): PublicCommentItem {
	return {
		id: 1,
		articleId: 34,
		content: '没有回复的评论',
		status: COMMENT_STATUS.APPROVED,
		moderationReason: null,
		author: {
			id: 10,
			username: 'user',
			nickname: '用户',
			avatarUrl: '',
			role: 'USER',
		},
		replyCount: 0,
		hasVisibleReplies: false,
		isMine: false,
		likeCount: 0,
		liked: false,
		createdAt: '2026-09-28T00:00:00Z',
	};
}

describe('PublicCommentSection', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		commentList.value = [];
		replyState.records = [];
		replyState.nextCursor = null;
		replyState.hasNext = false;
		replyState.loading = false;
		replyState.loaded = false;
		replyState.expanded = false;
		vi.mocked(useRoute).mockReturnValue({ query: reactive({ replyId: '51' }) } as never);
		vi.mocked(useRouter).mockReturnValue({ push: vi.fn() } as never);
		vi.mocked(useAuthStore).mockReturnValue({ accessToken: '', isLogin: false } as never);
		vi.mocked(useUserStore).mockReturnValue({ userInfo: null } as never);
		vi.mocked(usePublicCommentList).mockReturnValue({
			commentList,
			pageParams: reactive({ pageNum: 1, pageSize: 10, hasNext: false }),
			loading: ref(false),
			getReplyState: vi.fn(() => replyState),
			resetCommentList: vi.fn(),
			loadMoreComments: vi.fn(),
			getReplyList,
			collapseReplies,
			submitTopLevelComment: vi.fn(),
			submitReply: vi.fn(),
			deleteOwnComment: vi.fn(),
			removeTopLevelComment: vi.fn(),
			removeReply: vi.fn(),
			toggleCommentLike: vi.fn(),
		} as never);
	});

	it('does not render an empty reply list while locating a reply notification', async () => {
		const wrapper = mount(PublicCommentSection, {
			props: {
				article: {
					id: 34,
					allowComment: true,
					commentCount: 1,
				} as never,
			},
			global: { stubs },
		});

		commentList.value = [comment()];
		await nextTick();
		await nextTick();

		expect(getReplyList).not.toHaveBeenCalled();
		expect(wrapper.find('.reply-list').exists()).toBe(false);
		wrapper.unmount();
	});

	it.each([true, false])(
		'locates a rendered reply with allowComment=%s',
		async (allowComment) => {
			vi.mocked(useAuthStore).mockReturnValue({
				accessToken: 'token',
				isLogin: true,
			} as never);
			const wrapper = mount(PublicCommentSection, {
				attachTo: document.body,
				props: { article: { id: 34, allowComment, commentCount: 2 } as never },
				global: { stubs },
			});
			replyState.records = [
				{ ...comment(), id: 51, parentId: 1, rootId: 1, replyToUser: null },
			];
			commentList.value = [{ ...comment(), replyCount: 1, hasVisibleReplies: true }];
			await flushPromises();
			const target = wrapper.get('#comment-reply-51').element;
			expect(wrapper.emitted('replyLocated')?.[0]).toEqual([target]);
			expect(wrapper.find('public-comment-reply-editor-stub').exists()).toBe(allowComment);
			if (allowComment) {
				expect(
					wrapper.get('public-comment-reply-editor-stub').attributes('autofocus'),
				).toBe('true');
			}
			commentList.value = [...commentList.value, { ...comment(), id: 2 }];
			await flushPromises();
			expect(wrapper.emitted('replyLocated')).toHaveLength(1);
			wrapper.unmount();
		},
	);

	it('discards a notification lookup after its route changes', async () => {
		let resolveLookup!: () => void;
		getReplyList.mockImplementationOnce(
			() =>
				new Promise<void>((resolve) => {
					resolveLookup = resolve;
				}),
		);
		const route = { query: reactive({ replyId: '51' }) };
		vi.mocked(useRoute).mockReturnValue(route as never);
		const wrapper = mount(PublicCommentSection, {
			props: { article: { id: 34, allowComment: true, commentCount: 2 } as never },
			global: { stubs },
		});
		commentList.value = [{ ...comment(), replyCount: 1, hasVisibleReplies: true }];
		await nextTick();
		route.query.replyId = '';
		await nextTick();
		replyState.records = [{ ...comment(), id: 51, parentId: 1, rootId: 1, replyToUser: null }];
		resolveLookup();
		await flushPromises();
		expect(wrapper.emitted('replyLocated')).toBeUndefined();
		wrapper.unmount();
	});
});
