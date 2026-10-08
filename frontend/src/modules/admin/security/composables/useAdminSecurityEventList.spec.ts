import { beforeEach, describe, expect, it, vi } from 'vitest';
import { listSecurityEvents } from '../api/adminSecurityEventApi';
import { useAdminSecurityEventList } from './useAdminSecurityEventList';

vi.mock('../api/adminSecurityEventApi', () => ({ listSecurityEvents: vi.fn() }));

describe('useAdminSecurityEventList', () => {
	beforeEach(() => vi.clearAllMocks());

	it('loads security events and applies filters', async () => {
		vi.mocked(listSecurityEvents).mockResolvedValue({
			records: [],
			pageNum: 1,
			pageSize: 10,
			total: 0,
			totalPages: 0,
			hasNext: false,
		});
		const { filterForm, getSecurityEventList } = useAdminSecurityEventList();
		filterForm.userId = 10001;

		await getSecurityEventList();

		expect(listSecurityEvents).toHaveBeenCalledWith(expect.objectContaining({ userId: 10001 }));
	});
});
