import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getUnreadCount, listNotifications, markNotificationRead } from '../api/notificationApi';
import { useNotificationUnread } from '../composables/useNotifications';
import NotificationDialog from './NotificationDialog.vue';

vi.mock('../api/notificationApi', () => ({
	getUnreadCount: vi.fn(),
	listNotifications: vi.fn(),
	markAllNotificationsRead: vi.fn(),
	markNotificationRead: vi.fn(),
}));

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
};

describe('NotificationDialog', () => {
	beforeEach(() => {
		vi.clearAllMocks();
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
					read: false,
					createdAt: '2026-09-28T00:00:00Z',
				},
			],
		});
		vi.mocked(markNotificationRead).mockResolvedValue(null);
	});

	it('loads notifications when opened and marks an expanded item read', async () => {
		const wrapper = mount(NotificationDialog, {
			props: { modelValue: false },
			global: { stubs },
		});
		expect(listNotifications).not.toHaveBeenCalled();

		await wrapper.setProps({ modelValue: true });
		await flushPromises();
		expect(listNotifications).toHaveBeenCalledOnce();
		expect(wrapper.text()).not.toContain('回复内容');

		const toggle = wrapper.find('.notification-item__toggle');
		await toggle.trigger('click');
		await flushPromises();
		expect(wrapper.text()).toContain('回复内容');
		expect(toggle.attributes('aria-expanded')).toBe('true');
		expect(markNotificationRead).toHaveBeenCalledWith(1);

		await toggle.trigger('click');
		expect(wrapper.text()).not.toContain('回复内容');
		expect(toggle.attributes('aria-expanded')).toBe('false');
	});
});
