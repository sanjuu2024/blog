import { mount } from '@vue/test-utils';
import { defineComponent, h, inject, provide, reactive, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAdminAuditLogList } from '../composables/useAdminAuditLogList';
import {
	AUDIT_ACTION,
	AUDIT_RESOURCE_TYPE,
	AUDIT_RESULT,
	type AdminAuditLogItem,
} from '../types/adminAudit';
import AdminAuditLogListPage from './AdminAuditLogListPage.vue';

vi.mock('../composables/useAdminAuditLogList', () => ({
	useAdminAuditLogList: vi.fn(),
}));

vi.mock('@/utils/datetime', () => ({
	formatDateTime: vi.fn((date: string | null) => date || '-'),
}));

function log(overrides: Partial<AdminAuditLogItem> = {}): AdminAuditLogItem {
	return {
		id: 1,
		operatorId: 10001,
		operatorUsername: 'admin',
		resourceType: AUDIT_RESOURCE_TYPE.COMMENT,
		resourceId: '60001',
		action: AUDIT_ACTION.MODERATE,
		actionDetail: 'APPROVE',
		result: AUDIT_RESULT.SUCCESS,
		failureCode: null,
		failureMessage: null,
		requestMethod: 'PATCH',
		requestPath: '/api/v1/admin/comments/60001/moderation',
		createdAt: '2026-09-07T10:00:00Z',
		...overrides,
	};
}

const tableDataKey = Symbol('auditTableData');
const ElTableStub = defineComponent({
	props: { data: { type: Array<AdminAuditLogItem>, required: true } },
	setup(props, { slots }) {
		provide(tableDataKey, props.data);
		return () => h('div', slots.default?.());
	},
});
const ElTableColumnStub = defineComponent({
	setup(_, { slots }) {
		const data = inject<AdminAuditLogItem[]>(tableDataKey, []);
		return () =>
			h(
				'div',
				data.map((row) => slots.default?.({ row })),
			);
	},
});

describe('AdminAuditLogListPage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(useAdminAuditLogList).mockReturnValue({
			auditLogList: ref([
				log({ id: 1, requestMethod: 'PUT' }),
				log({ id: 2, requestMethod: 'PATCH' }),
				log({ id: 3, requestMethod: 'DELETE' }),
			]),
			loading: ref(false),
			pageMeta: reactive({ total: 1, totalPages: 1, hasNext: false }),
			pageParams: reactive({ pageNum: 1, pageSize: 10 }),
			filterForm: reactive({
				operatorId: undefined,
				resourceType: '',
				resourceId: '',
				action: '',
				result: '',
				createdAtRange: [],
			}),
			getAuditLogList: vi.fn(),
			resetFilterForm: vi.fn(),
		});
	});

	it('renders audit fields and exposes the full resource identifier on hover', () => {
		const wrapper = mount(AdminAuditLogListPage, {
			global: {
				stubs: {
					ElCard: { template: '<section><slot name="header" /><slot /></section>' },
					ElForm: { template: '<form><slot /></form>' },
					ElInput: true,
					ElInputNumber: true,
					ElSelect: { template: '<div><slot /></div>' },
					ElOption: true,
					ElDatePicker: true,
					ElButton: { template: '<button><slot /></button>' },
					ElTable: ElTableStub,
					ElTableColumn: ElTableColumnStub,
					ElTag: { template: '<span><slot /></span>' },
					ElTooltip: {
						props: ['content'],
						template:
							'<span class="tooltip-stub" :data-content="content"><slot /></span>',
					},
					ElPagination: true,
					ILetsIconsSearchAlt: true,
					ILetsIconsRefresh: true,
				},
			},
		});

		expect(wrapper.text()).toContain('admin');
		expect(wrapper.text()).toContain('审核处理');
		expect(wrapper.text()).toContain('成功');
		expect(wrapper.text()).toContain('/api/v1/admin/comments/60001/moderation');
		expect(wrapper.find('.admin-audit-log__result-tag').attributes('effect')).toBe('plain');
		expect(wrapper.find('.admin-audit-log__method--put').text()).toBe('PUT');
		expect(wrapper.find('.admin-audit-log__method--patch').text()).toBe('PATCH');
		expect(wrapper.find('.admin-audit-log__method--delete').text()).toBe('DELETE');
		const resourceId = wrapper.find('.admin-audit-log__resource-id');
		expect(resourceId.text()).toBe('60001');
		expect(resourceId.element.parentElement?.dataset.content).toBe('60001');
		expect(wrapper.find('.admin-audit-log__ellipsis').attributes('title')).toBe(
			'/api/v1/admin/comments/60001/moderation',
		);
	});

	it('renders a placeholder when the resource identifier is absent', () => {
		vi.mocked(useAdminAuditLogList).mockReturnValue({
			auditLogList: ref([log({ resourceId: null })]),
			loading: ref(false),
			pageMeta: reactive({ total: 1, totalPages: 1, hasNext: false }),
			pageParams: reactive({ pageNum: 1, pageSize: 10 }),
			filterForm: reactive({
				operatorId: undefined,
				resourceType: '',
				resourceId: '',
				action: '',
				result: '',
				createdAtRange: [],
			}),
			getAuditLogList: vi.fn(),
			resetFilterForm: vi.fn(),
		});
		const wrapper = mount(AdminAuditLogListPage, {
			global: {
				stubs: {
					ElCard: { template: '<section><slot name="header" /><slot /></section>' },
					ElForm: { template: '<form><slot /></form>' },
					ElInput: true,
					ElInputNumber: true,
					ElSelect: { template: '<div><slot /></div>' },
					ElOption: true,
					ElDatePicker: true,
					ElButton: { template: '<button><slot /></button>' },
					ElTable: ElTableStub,
					ElTableColumn: ElTableColumnStub,
					ElTag: { template: '<span><slot /></span>' },
					ElTooltip: {
						props: ['content'],
						template:
							'<span class="tooltip-stub" :data-content="content"><slot /></span>',
					},
					ElPagination: true,
					ILetsIconsSearchAlt: true,
					ILetsIconsRefresh: true,
				},
			},
		});

		expect(wrapper.text()).toContain('未生成资源 ID');
		expect(wrapper.find('.admin-audit-log__resource-id').exists()).toBe(false);
	});
});
