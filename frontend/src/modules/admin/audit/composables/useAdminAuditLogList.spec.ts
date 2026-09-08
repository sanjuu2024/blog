import { beforeEach, describe, expect, it, vi } from 'vitest';
import { listAuditLogs } from '../api/adminAuditApi';
import {
	AUDIT_ACTION,
	AUDIT_RESOURCE_TYPE,
	AUDIT_RESULT,
	type AdminAuditLogPageData,
} from '../types/adminAudit';
import { useAdminAuditLogList } from './useAdminAuditLogList';

vi.mock('../api/adminAuditApi', () => ({
	listAuditLogs: vi.fn(),
}));

function page(): AdminAuditLogPageData {
	return {
		total: 0,
		totalPages: 0,
		pageNum: 1,
		pageSize: 10,
		hasNext: false,
		records: [],
	};
}

describe('useAdminAuditLogList', () => {
	beforeEach(() => vi.clearAllMocks());

	it('builds all audit log filters', async () => {
		vi.mocked(listAuditLogs).mockResolvedValue(page());
		const { filterForm, getAuditLogList } = useAdminAuditLogList();
		filterForm.operatorId = 10001;
		filterForm.resourceType = AUDIT_RESOURCE_TYPE.COMMENT;
		filterForm.resourceId = ' 60001 ';
		filterForm.action = AUDIT_ACTION.MODERATE;
		filterForm.result = AUDIT_RESULT.FAILURE;
		filterForm.createdAtRange = ['2026-09-01T00:00:00Z', '2026-09-30T23:59:59Z'];

		await getAuditLogList(2);

		expect(listAuditLogs).toHaveBeenCalledWith({
			pageNum: 2,
			pageSize: 10,
			operatorId: 10001,
			resourceType: AUDIT_RESOURCE_TYPE.COMMENT,
			resourceId: '60001',
			action: AUDIT_ACTION.MODERATE,
			result: AUDIT_RESULT.FAILURE,
			createdAtFrom: '2026-09-01T00:00:00Z',
			createdAtTo: '2026-09-30T23:59:59Z',
		});
	});

	it('keeps the newest response when requests finish out of order', async () => {
		let resolveFirst!: (data: AdminAuditLogPageData) => void;
		vi.mocked(listAuditLogs)
			.mockReturnValueOnce(
				new Promise((resolve) => {
					resolveFirst = resolve;
				}),
			)
			.mockResolvedValueOnce({ ...page(), pageNum: 2, total: 1 });
		const { auditLogList, loading, getAuditLogList } = useAdminAuditLogList();

		const firstRequest = getAuditLogList(1);
		await getAuditLogList(2);
		resolveFirst({ ...page(), pageNum: 1, total: 9 });
		await firstRequest;

		expect(auditLogList.value).toEqual([]);
		expect(loading.value).toBe(false);
	});
});
