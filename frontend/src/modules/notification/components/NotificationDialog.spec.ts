import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useRouter } from 'vue-router';
import { getUnreadCount, listNotifications, markNotificationRead } from '../api/notificationApi';
import { createComment } from '@/modules/comment/api/commentApi';
import { useNotificationUnread } from '../composables/useNotifications';
import NotificationDialog from './NotificationDialog.vue';

vi.mock('vue-router', async (importOriginal) => ({
	...(await importOriginal<typeof import('vue-router')>()),
	useRouter: vi.fn(),
}));

vi.mock('../api/notificationApi', () => ({
	getUnreadCount: vi.fn(),
	listNotifications: vi.fn(),
	markAllNotificationsRead: vi.fn(),
	markNotificationRead: vi.fn(),
}));
vi.mock('@/modules/comment/api/commentApi', () => ({
	createComment: vi.fn(),
}));
vi.mock('@/stores/userStore', () => ({
	useUserStore: vi.fn(() => ({ userInfo: null })),
}));

const ReplyEditorStub = defineComponent({
	props: { modelValue: { type: String, default: '' } },
	emits: ['update:modelValue', 'cancel', 'submit'],
	setup(props, { emit }) {
		return () =>
			h('div', { 'data-testid': 'reply-editor' }, [
				h('input', {
					'data-testid': 'reply-editor-input',
					value: props.modelValue,
					onInput: (event: Event) =>
						emit('update:modelValue', (event.target as HTMLInputElement).value),
				}),
				h(
					'button',
					{ 'data-testid': 'reply-editor-submit', onClick: () => emit('submit') },
					'提交回复',
				),
			]);
	},
});

const stubs = {
	ElDialog: {
		props: ['modelValue'],
		template: '<div v-if="modelValue"><slot /></div>',
	},
	ElButton: { template: '<button><slot /></button>' },
	ElRadioGroup: { template: '<div><slot /></div>' },
	ElRadioButton: { template: '<span><slot /></span>' },
	ElEmpty: true,
	ILucideChevronRight: true,
	AppLoadMoreTrigger: true,
	PublicCommentReplyEditor: ReplyEditorStub,
};

describe('NotificationDialog', () => {
	const push = vi.fn();

	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(useRouter).mockReturnValue({ push } as unknown as ReturnType<typeof useRouter>);
		useNotificationUnread().clearUnreadCount();
		vi.mocked(getUnreadCount).mockResolvedValue({ total: 1, reply: 1, adminMessage: 0 });
		vi.mocked(listNotifications).mockResolvedValue({
			total: 1,
			totalPages: 1,
			pageNum: 1,
			pageSize: 20,
			hasNext: false,
			records: [
				{
					id: 1,
					type: 'COMMENT_REPLY',
					title: '评论收到新的回复',
					content: '回复内容',
					sourceId: 30001,
					authorName: '回复者',
					originalContent: '我的评论',
					articleId: 40001,
					parentId: 30000,
					likeCount: 0,
					liked: false,
					canInteract: true,
					read: false,
					createdAt: '2026-09-28T00:00:00Z',
				},
			],
		});
		vi.mocked(markNotificationRead).mockResolvedValue(null);
	});

	it('loads notifications with the message content visible', async () => {
		const wrapper = mount(NotificationDialog, {
			props: { modelValue: false },
			global: { stubs },
		});
		expect(listNotifications).not.toHaveBeenCalled();

		await wrapper.setProps({ modelValue: true });
		await flushPromises();
		expect(listNotifications).toHaveBeenCalledOnce();
		expect(wrapper.text()).toContain('@回复者回复了我的评论');
		expect(wrapper.text()).toContain('回复内容');
		expect(wrapper.text()).toContain('我的评论');
		expect(wrapper.text()).toContain('2026-09-28');
	});

	it('navigates to the article comment section from a reply notification', async () => {
		const wrapper = mount(NotificationDialog, {
			props: { modelValue: true },
			global: { stubs },
		});
		await flushPromises();
		await wrapper.find('.notification-item__content').trigger('click');

		expect(push).toHaveBeenCalledWith({
			name: 'ArticleDetail',
			params: { articleId: 40001 },
			hash: '#article-comments',
			query: { replyId: '30001' },
		});
		expect(wrapper.emitted('update:modelValue')).toEqual([[false]]);
	});

	it('opens and submits an inline comment reply editor', async () => {
		vi.mocked(createComment).mockResolvedValue({ status: 'APPROVED' } as never);
		const wrapper = mount(NotificationDialog, {
			props: { modelValue: true },
			global: { stubs },
		});
		await flushPromises();

		await wrapper.find('button[aria-label="快捷回复"]').trigger('click');
		const input = wrapper.find('[data-testid="reply-editor-input"]');
		await input.setValue('通知内回复');
		await wrapper.find('[data-testid="reply-editor-submit"]').trigger('click');
		await flushPromises();

		expect(createComment).toHaveBeenCalledWith(40001, {
			content: '通知内回复',
			parentId: 30001,
		});
	});

	it('navigates to the original message from a message reply notification', async () => {
		vi.mocked(listNotifications).mockResolvedValueOnce({
			total: 1,
			totalPages: 1,
			pageNum: 1,
			pageSize: 20,
			hasNext: false,
			records: [
				{
					id: 2,
					type: 'MESSAGE_REPLY',
					title: '留言回复',
					content: '回复内容',
					sourceId: 90002,
					authorName: '管理员',
					originalContent: '我的留言',
					articleId: null,
					parentId: 90001,
					read: false,
					createdAt: '',
				},
			],
		});
		const wrapper = mount(NotificationDialog, {
			props: { modelValue: true },
			global: { stubs },
		});
		await flushPromises();
		await wrapper.find('.notification-item__content').trigger('click');

		expect(push).toHaveBeenCalledWith({
			name: 'MessageBoard',
			query: { messageId: '90001' },
		});
	});
});
