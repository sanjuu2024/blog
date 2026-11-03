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
			recipientMasked: 't***@example.com',
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
});
