import { mount } from '@vue/test-utils';
import { defineComponent, h, inject, provide, reactive, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { formatDateTime } from '@/utils/datetime';
import { useAdminMessageList } from '../composables/useAdminMessageList';
import { MESSAGE_STATUS, MESSAGE_TYPE, type AdminMessageItem } from '../types/adminMessage';
import AdminMessageListPage from './AdminMessageListPage.vue';

vi.mock('../composables/useAdminMessageList', () => ({
	useAdminMessageList: vi.fn(),
}));

vi.mock('@/utils/datetime', () => ({
	formatDateTime: vi.fn((date: string | null) => date || '-'),
}));

const tableDataKey = Symbol('tableData');
const ElTableStub = defineComponent({
	props: { data: { type: Array<AdminMessageItem>, required: true } },
	setup(props, { slots }) {
		provide(tableDataKey, props.data);
		return () => h('div', slots.default?.());
	},
});
const ElTableColumnStub = defineComponent({
	setup(_, { slots }) {
		const data = inject<AdminMessageItem[]>(tableDataKey, []);
		return () =>
			h(
				'div',
				data.map((row) => slots.default?.({ row })),
			);
	},
});

function message(overrides: Partial<AdminMessageItem>): AdminMessageItem {
	return {
		id: 90001,
		userId: null,
		parentId: null,
		nickname: '访客',
		email: '',
		content: '留言内容',
		status: MESSAGE_STATUS.APPROVED,
		type: MESSAGE_TYPE.TOP_LEVEL,
		notifyOnReply: false,
		moderationReason: null,
		reviewedBy: null,
		reviewedAt: null,
		deletedBy: null,
		deletedAt: null,
		author: null,
		createdAt: '2026-08-12T00:00:00+08:00',
		...overrides,
	};
}

describe('AdminMessageListPage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(useAdminMessageList).mockReturnValue({
			messageList: ref([
				message({
					id: 90001,
					status: MESSAGE_STATUS.DELETED,
					reviewedAt: '2026-08-10T09:00:00+08:00',
					deletedAt: '2026-08-12T10:00:00+08:00',
				}),
				message({
					id: 90002,
					status: MESSAGE_STATUS.REJECTED,
					reviewedAt: '2026-08-11T11:00:00+08:00',
					deletedAt: '2026-08-13T12:00:00+08:00',
				}),
			]),
			selectedMessageIds: ref<number[]>([]),
			submitting: ref(false),
			pageMeta: reactive({ total: 2, totalPages: 1, hasNext: false }),
			pageParams: reactive({ pageNum: 1, pageSize: 10 }),
			filterForm: reactive({
				messageId: undefined,
				userId: undefined,
				guestNickname: '',
				guestEmail: '',
				content: '',
				status: '',
				type: '',
				createdAtRange: [],
			}),
			getMessageList: vi.fn(),
			resetFilterForm: vi.fn(),
			handleModerate: vi.fn(),
			reply: vi.fn(),
			approveSelected: vi.fn(),
			canApprove: vi.fn(() => false),
			canReply: vi.fn(() => false),
			canReject: vi.fn(() => false),
			canHide: vi.fn(() => false),
			canDelete: vi.fn(() => false),
		});
	});

	it('uses deletedAt for deleted messages and reviewedAt for other reviewed states', () => {
		mount(AdminMessageListPage, {
			global: {
				stubs: {
					ElCard: { template: '<section><slot name="header" /><slot /></section>' },
					ElForm: { template: '<form><slot /></form>' },
					ElFormItem: { template: '<div><slot /></div>' },
					ElTable: ElTableStub,
					ElTableColumn: ElTableColumnStub,
					ElInput: true,
					ElInputNumber: true,
					ElButton: { template: '<button><slot /></button>' },
					ElDatePicker: true,
					ElRadioGroup: { template: '<div><slot /></div>' },
					ElRadio: { template: '<span><slot /></span>' },
					ElTag: { template: '<span><slot /></span>' },
					ElPagination: true,
					ElDialog: true,
					ILetsIconsCheckFill: true,
					ILetsIconsSearchAlt: true,
					ILetsIconsRefresh: true,
				},
			},
		});

		expect(formatDateTime).toHaveBeenCalledWith('2026-08-12T10:00:00+08:00');
		expect(formatDateTime).toHaveBeenCalledWith('2026-08-11T11:00:00+08:00');
		expect(formatDateTime).not.toHaveBeenCalledWith('2026-08-10T09:00:00+08:00');
		expect(formatDateTime).not.toHaveBeenCalledWith('2026-08-13T12:00:00+08:00');
	});
});
