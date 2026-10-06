import { mount } from '@vue/test-utils';
import { nextTick, reactive, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { useUserStore } from '@/stores/userStore';
import PublicCommentSection from './PublicCommentSection.vue';
import { usePublicCommentList } from '../composables/usePublicCommentList';
import { COMMENT_STATUS, type PublicCommentItem } from '../types/comment';

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
const replyState = reactive({
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
	});
});
