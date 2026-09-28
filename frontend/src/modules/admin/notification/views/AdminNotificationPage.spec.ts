import { mount, flushPromises } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createAdminNotification, listAdminNotifications } from '../api/adminNotificationApi';
import AdminNotificationPage from './AdminNotificationPage.vue';

vi.mock('../api/adminNotificationApi', () => ({
	createAdminNotification: vi.fn(),
	listAdminNotifications: vi.fn(),
	updateAdminNotification: vi.fn(),
	updateAdminNotificationStatus: vi.fn(),
}));

const ButtonStub = defineComponent({
	emits: ['click'],
	setup(_, { emit, slots }) {
		return () => h('button', { onClick: () => emit('click') }, slots.default?.());
	},
});

const InputStub = defineComponent({
	props: { modelValue: { type: String, default: '' } },
	emits: ['update:modelValue'],
	setup(props, { emit }) {
		return () =>
			h('input', {
				value: props.modelValue,
				onInput: (event: Event) =>
					emit('update:modelValue', (event.target as HTMLInputElement).value),
			});
	},
});

describe('AdminNotificationPage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(listAdminNotifications).mockResolvedValue([]);
		vi.mocked(createAdminNotification).mockResolvedValue({
			id: 1,
			title: '标题',
			content: '正文',
			targetScope: 'ALL_USERS',
			userIds: [],
			status: 'DRAFT',
			createdAt: '2026-09-28T00:00:00Z',
			publishedAt: null,
		});
	});

	it('sends DRAFT when saving a new message draft', async () => {
		const wrapper = mount(AdminNotificationPage, {
			global: {
				stubs: {
					ElButton: ButtonStub,
					ElTable: true,
					ElTableColumn: true,
					ElDialog: { template: '<div><slot /><slot name="footer" /></div>' },
					ElForm: { template: '<form><slot /></form>' },
					ElFormItem: { template: '<div><slot /></div>' },
					ElInput: InputStub,
					ElRadioGroup: { template: '<div><slot /></div>' },
					ElRadio: true,
					ILucidePlus: true,
				},
			},
		});
		await flushPromises();
		const inputs = wrapper.findAll('input');
		await inputs[0]?.setValue('标题');
		await inputs[1]?.setValue('正文');
		await wrapper
			.findAll('button')
			.find((button) => button.text() === '保存草稿')
			?.trigger('click');
		await flushPromises();

		expect(createAdminNotification).toHaveBeenCalledWith({
			targetScope: 'ALL_USERS',
			title: '标题',
			content: '正文',
			status: 'DRAFT',
			userIds: undefined,
		});
	});
});
