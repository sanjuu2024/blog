import { describe, expect, it, vi } from 'vitest';
import request from '@/utils/request';
import { getSeoMetadata } from './seoApi';

vi.mock('@/utils/request', () => ({ default: { get: vi.fn() } }));

describe('seoApi', () => {
	it('queries a canonical path without showing ancillary error messages', async () => {
		vi.mocked(request.get).mockResolvedValue({ title: '青禾边' });
		await getSeoMetadata('/articles/34');
		expect(request.get).toHaveBeenCalledWith('seo', {
			params: { path: '/articles/34' },
			meta: { showError: false },
		});
	});
});
