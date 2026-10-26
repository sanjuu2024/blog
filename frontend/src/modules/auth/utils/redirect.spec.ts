import { createMemoryHistory, createRouter } from 'vue-router';
import { describe, expect, it } from 'vitest';
import { routes } from '@/router/routes';
import { getAuthRedirect } from './redirect';

const router = createRouter({ history: createMemoryHistory(), routes });

describe('getAuthRedirect', () => {
	it.each([
		'/articles?keyword=Vue&tagIds=3',
		'/articles/34?replyId=51#article-comments',
		'/users/me',
		'/admin/dashboard',
		'/',
	])('preserves the valid internal target %s', (target) => {
		expect(getAuthRedirect(router, target)).toBe(target);
	});

	it.each([
		undefined,
		null,
		'',
		[],
		'https://example.com',
		'//example.com',
		'/\\example.com',
		'/articles\n',
		'/missing-page',
		'/auth',
		'/auth/login?redirect=%2Farticles',
		'/auth/register#form',
		'/auth/login/',
	])('rejects the invalid or authentication target %s', (target) => {
		expect(getAuthRedirect(router, target)).toBeUndefined();
	});

	it('uses only the first redirect query value', () => {
		expect(getAuthRedirect(router, ['/messages', '/articles'])).toBe('/messages');
		expect(getAuthRedirect(router, ['//example.com', '/articles'])).toBeUndefined();
	});
});
