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

const copy = vi.hoisted(() => vi.fn());
const showSuccess = vi.hoisted(() => vi.fn());
const showError = vi.hoisted(() => vi.fn());

vi.mock('@vueuse/core', async (importOriginal) => ({
	...(await importOriginal<typeof import('@vueuse/core')>()),
	useClipboard: () => ({ copy }),
}));

vi.mock('element-plus', async (importOriginal) => ({
	...(await importOriginal<typeof import('element-plus')>()),
	ElMessage: {
		success: showSuccess,
		error: showError,
	},
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
		copy.mockResolvedValue(undefined);
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

	it('renders audit fields and copies the resource identifier', async () => {
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
		expect(resourceId.attributes('title')).toBe('点击复制：60001');
		await resourceId.trigger('click');
		expect(copy).toHaveBeenCalledWith('60001');
		expect(showSuccess).toHaveBeenCalledWith('目标资源标识已复制');
		expect(wrapper.find('.admin-audit-log__ellipsis').attributes('title')).toBe(
			'/api/v1/admin/comments/60001/moderation',
		);
	});

	it('shows an error when copying the resource identifier fails', async () => {
		copy.mockRejectedValueOnce(new Error('clipboard unavailable'));
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
					ElPagination: true,
					ILetsIconsSearchAlt: true,
					ILetsIconsRefresh: true,
				},
			},
		});

		await wrapper.find('.admin-audit-log__resource-id').trigger('click');
		await Promise.resolve();
		expect(showError).toHaveBeenCalledWith('复制失败，请手动复制');
	});
});
