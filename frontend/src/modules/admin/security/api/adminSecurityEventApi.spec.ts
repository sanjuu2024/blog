import { beforeEach, describe, expect, it, vi } from 'vitest';
import request from '@/utils/request';
import { listSecurityEvents } from './adminSecurityEventApi';

vi.mock('@/utils/request', () => ({
	default: { get: vi.fn() },
}));

describe('admin security event API', () => {
	beforeEach(() => vi.clearAllMocks());

	it('sends filters as query parameters', () => {
		const params = { pageNum: 1, pageSize: 10, userId: 10001 };

		listSecurityEvents(params);

		expect(request.get).toHaveBeenCalledWith('admin/security-events', { params });
	});
});
