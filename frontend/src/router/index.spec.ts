import { describe, expect, it } from 'vitest';
import router from './index';

const scrollBehavior = router.options.scrollBehavior!;

function resolveRoute(path: string) {
	const route = router.resolve(path);
	return { ...route, name: route.name ?? undefined };
}

describe('router scroll behavior', () => {
	it.each([
		['/articles', '/articles?keyword=Vue'],
		['/articles?keyword=Vue', '/articles?keyword=Spring'],
		['/articles', '/articles?categoryId=21001'],
		['/articles', '/articles?tagIds=30001&tagIds=30002'],
		['/articles?keyword=Vue&categoryId=21001&tagIds=30001', '/articles'],
		['/articles?keyword=Vue#results', '/articles?keyword=Spring#results'],
	])('preserves scrolling when filters change from %s to %s', (from, to) => {
		expect(scrollBehavior(resolveRoute(to), resolveRoute(from), null)).toBe(false);
	});

	it.each([
		['/home', '/articles?keyword=Vue'],
		['/articles?keyword=Vue', '/home'],
		['/articles/40001', '/articles/40002'],
		['/articles?keyword=Vue', '/articles?keyword=Vue#results'],
	])('scrolls to the top for navigation from %s to %s', (from, to) => {
		expect(scrollBehavior(resolveRoute(to), resolveRoute(from), null)).toEqual({
			left: 0,
			top: 0,
		});
	});
});
