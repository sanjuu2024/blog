import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { defineComponent, nextTick, reactive, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '@/stores/authStore';
import { useUserStore } from '@/stores/userStore';
import { USER_ROLE, USER_STATUS } from '@/modules/user/types/user';
import { usePublicMessageList } from '../composables/usePublicMessageList';
import { MESSAGE_STATUS, type PublicMessageItem } from '../types/message';
import MessageBoardPage from './MessageBoardPage.vue';

vi.mock('../composables/usePublicMessageList', () => ({
	usePublicMessageList: vi.fn(),
}));

function privateMessage(): PublicMessageItem {
	return {
		id: 90001,
		nickname: '登录用户',
		content: '待审核留言',
		status: MESSAGE_STATUS.PENDING,
		moderationReason: null,
		author: null,
		isMine: true,
		replies: [],
		createdAt: '2026-08-12T00:00:00Z',
	};
}

describe('MessageBoardPage', () => {
	beforeEach(() => {
		setActivePinia(createPinia());
		vi.clearAllMocks();
	});

	it('clears private messages and reloads the first page after logout', async () => {
		const messageList = ref<PublicMessageItem[]>([privateMessage()]);
		const getMessageList = vi.fn().mockResolvedValue(undefined);
		vi.mocked(usePublicMessageList).mockReturnValue({
			messageList,
			loading: ref(false),
			pageParams: reactive({ pageNum: 1, pageSize: 10, hasNext: false }),
			getMessageList,
			loadMoreMessages: vi.fn(),
			submitMessage: vi.fn(),
			removeMessage: vi.fn(),
		});

		const authStore = useAuthStore();
		const userStore = useUserStore();
		authStore.accessToken = 'access-token';
		userStore.userInfo = {
			id: 10002,
			username: 'message_user',
			nickname: '登录用户',
			email: 'message@example.com',
			role: USER_ROLE.USER,
			status: USER_STATUS.ACTIVE,
			avatarUrl: '',
			bio: '',
		};

		mount(MessageBoardPage, {
			global: {
				stubs: {
					MessageEditor: true,
					MessageListItem: true,
					AppLoadMoreTrigger: true,
					ILucideMessageCircleMore: true,
				},
			},
		});
		await nextTick();
		expect(getMessageList).toHaveBeenCalledTimes(1);

		userStore.clearUserInfo();
		await nextTick();

		expect(messageList.value).toEqual([]);
		expect(getMessageList).toHaveBeenLastCalledWith(1);
	});

	it('resets only the Turnstile challenge when submission fails', async () => {
		const resetTurnstile = vi.fn();
		const clear = vi.fn();
		const submitMessage = vi.fn().mockRejectedValue(new Error('request failed'));
		vi.mocked(usePublicMessageList).mockReturnValue({
			messageList: ref([]),
			loading: ref(false),
			pageParams: reactive({ pageNum: 1, pageSize: 10, hasNext: false }),
			getMessageList: vi.fn().mockResolvedValue(undefined),
			loadMoreMessages: vi.fn(),
			submitMessage,
			removeMessage: vi.fn(),
		});
		const messageEditorStub = defineComponent({
			emits: ['submit'],
			setup(_props, { expose }) {
				expose({ clear, resetTurnstile });
			},
			template:
				"<button data-test=\"submit\" @click=\"$emit('submit', { nickname: '访客', content: '留言内容', turnstileToken: 'token' })\">提交</button>",
		});

		const wrapper = mount(MessageBoardPage, {
			global: {
				config: { errorHandler: () => undefined },
				stubs: {
					MessageEditor: messageEditorStub,
					MessageListItem: true,
					AppLoadMoreTrigger: true,
					ILucideMessageCircleMore: true,
				},
			},
		});
		await wrapper.get('[data-test="submit"]').trigger('click');
		await flushPromises();

		expect(submitMessage).toHaveBeenCalledWith({
			nickname: '访客',
			content: '留言内容',
			turnstileToken: 'token',
		});
		expect(resetTurnstile).toHaveBeenCalledOnce();
		expect(clear).not.toHaveBeenCalled();
	});

	it('hides the end-of-list message when there are no messages', async () => {
		const messageList = ref<PublicMessageItem[]>([]);
		vi.mocked(usePublicMessageList).mockReturnValue({
			messageList,
			loading: ref(false),
			pageParams: reactive({ pageNum: 1, pageSize: 10, hasNext: false }),
			getMessageList: vi.fn().mockResolvedValue(undefined),
			loadMoreMessages: vi.fn(),
			submitMessage: vi.fn(),
			removeMessage: vi.fn(),
		});

		const loadMoreStub = defineComponent({
			props: { showNoMore: { type: Boolean, required: true } },
			template: '<div data-test="load-more">{{ showNoMore ? "没有更多留言了" : "" }}</div>',
		});
		const wrapper = mount(MessageBoardPage, {
			global: {
				stubs: {
					MessageEditor: true,
					MessageListItem: true,
					AppLoadMoreTrigger: loadMoreStub,
					ILucideMessageCircleMore: true,
				},
			},
		});
		await nextTick();

		expect(wrapper.get('.message-board__header').text()).toBe('留言板');
		expect(wrapper.get('.message-board__empty').text()).toBe('还没有留言，来说点什么吧。');
		expect(wrapper.get('[data-test="load-more"]').text()).toBe('');
	});
});
