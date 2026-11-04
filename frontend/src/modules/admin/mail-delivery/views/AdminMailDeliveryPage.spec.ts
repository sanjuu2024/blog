import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { formatDateTime } from '@/utils/datetime';
import { listMailDeliveries } from '../api/adminMailDeliveryApi';
import type { AdminMailDeliveryItem } from '../types/adminMailDelivery';
import AdminMailDeliveryPage from './AdminMailDeliveryPage.vue';

vi.mock('@vueuse/core', () => ({
	useMediaQuery: () => ({ value: false }),
}));

vi.mock('../api/adminMailDeliveryApi', () => ({
	getMailDelivery: vi.fn(),
	listMailDeliveries: vi.fn(),
	retryMailDelivery: vi.fn(),
}));

describe('AdminMailDeliveryPage', () => {
	let mailDelivery: AdminMailDeliveryItem;

	beforeEach(() => {
		mailDelivery = {
			id: 1,
			mailType: 'MESSAGE_REPLY',
			sourceId: 10,
			replyId: 11,
			recipientEmail: 'test@example.com',
			recipientMasked: null,
			currentRecipientEmail: null,
			userId: 10002,
			userUsername: 'test-user',
			userNickname: '测试用户',
			userDeleted: false,
			status: 'SENT',
			attemptCount: 1,
			lastErrorType: null,
			lastErrorMessage: null,
			sentAt: '2026-11-03T10:05:00Z',
			lastAttemptAt: '2026-11-03T10:05:00Z',
			createdAt: '2026-11-03T10:04:00Z',
		};
		vi.mocked(listMailDeliveries).mockResolvedValue({
			total: 1,
			totalPages: 1,
			pageNum: 1,
			pageSize: 10,
			hasNext: false,
			records: [mailDelivery],
		});
	});

	function mountPage() {
		return mount(AdminMailDeliveryPage, {
			global: {
				stubs: {
					ElCard: { template: '<section><slot name="header" /><slot /></section>' },
					ElForm: { template: '<form><slot /></form>' },
					ElFormItem: { template: '<div><slot /></div>' },
					ElSelect: true,
					ElOption: true,
					ElButton: { template: '<button><slot /></button>' },
					ElTable: { template: '<div><slot /></div>' },
					ElTableColumn: {
						props: ['label'],
						data: () => ({ row: mailDelivery }),
						template: '<div :data-label="label"><slot :row="row" /></div>',
					},
					ElTag: { template: '<span><slot /></span>' },
					ElPagination: true,
					ElDialog: true,
					ElSkeleton: true,
					ElDescriptions: true,
					ElDescriptionsItem: true,
					ElEmpty: true,
				},
			},
		});
	}

	it('shows the last attempt time in the delivery list', async () => {
		const wrapper = mountPage();
		await flushPromises();

		const lastAttemptColumn = wrapper.get('[data-label="最近尝试时间"]');
		expect(lastAttemptColumn.text()).toContain(formatDateTime(mailDelivery.lastAttemptAt));
	});

	it('shows a placeholder when no delivery attempt has been made', async () => {
		mailDelivery.lastAttemptAt = null;
		const wrapper = mountPage();
		await flushPromises();

		expect(wrapper.get('[data-label="最近尝试时间"]').text()).toBe('-');
	});

	it('shows the full recipient address and the current user profile', async () => {
		const wrapper = mountPage();
		await flushPromises();

		expect(wrapper.get('[data-label="收件地址"]').text()).toBe('test@example.com');
		const user = wrapper.get('[data-label="收件用户"]');
		expect(user.text()).toContain('测试用户');
		expect(user.text()).toContain('test-user');
		expect(user.text()).toContain('10002');
	});

	it('shows a deleted identity without reusing the previous nickname', async () => {
		mailDelivery.userDeleted = true;
		mailDelivery.userUsername = 'deleted_10002';
		const wrapper = mountPage();
		await flushPromises();

		expect(wrapper.get('[data-label="收件用户"]').text()).toContain('账号已注销');
		expect(wrapper.get('[data-label="收件用户"]').text()).toContain('deleted_10002');
		expect(wrapper.get('[data-label="收件用户"]').text()).not.toContain('测试用户');
	});

	it('shows guests and explicitly identifies missing historical addresses', async () => {
		mailDelivery.userId = null;
		mailDelivery.recipientEmail = null;
		const wrapper = mountPage();
		await flushPromises();

		expect(wrapper.get('[data-label="收件用户"]').text()).toBe('游客');
		expect(wrapper.get('[data-label="收件地址"]').text()).toBe('未记录');
	});

	it('keeps the historical masked address and labels the current address as a reference', async () => {
		mailDelivery.recipientEmail = null;
		mailDelivery.recipientMasked = 'o***@example.com';
		mailDelivery.currentRecipientEmail = 'current@example.com';
		const wrapper = mountPage();
		await flushPromises();

		const address = wrapper.get('[data-label="收件地址"]');
		expect(address.text()).toContain('current@example.com');
		expect(address.text()).toContain('当前邮箱，历史地址未记录');
		expect(address.text()).toContain('历史脱敏地址：o***@example.com');
	});

	it('prioritizes the recorded recipient over a changed current address', async () => {
		mailDelivery.currentRecipientEmail = 'current@example.com';
		const wrapper = mountPage();
		await flushPromises();

		expect(wrapper.get('[data-label="收件地址"]').text()).toBe('test@example.com');
	});
});
