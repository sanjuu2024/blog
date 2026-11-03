import { beforeEach, describe, expect, it, vi } from 'vitest';
import request from '@/utils/request';
import { getArchiveArticles, getArchiveYears } from './archiveApi';

vi.mock('@/utils/request', () => ({ default: { get: vi.fn() } }));

describe('archive API contracts', () => {
	beforeEach(() => vi.clearAllMocks());

	it('sends the same browser time zone with year summaries and article pagination', () => {
		const timeZone = 'America/Los_Angeles';
		const controller = new AbortController();
		const params = { timeZone, year: 2026, pageNum: 1, pageSize: 10 };

		getArchiveYears(timeZone);
		getArchiveArticles(params, controller.signal);

		expect(request.get).toHaveBeenNthCalledWith(1, 'archives/years', {
			params: { timeZone },
		});
		expect(request.get).toHaveBeenNthCalledWith(2, 'archives/articles', {
			params,
			signal: controller.signal,
		});
	});
});
