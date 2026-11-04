import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import router from './index';
import {
	markArticleListReady,
	prepareArticleListScroll,
	setPageViewPending,
} from './articleListScroll';

const scrollBehavior = router.options.scrollBehavior!;

function resolveRoute(path: string) {
	const route = router.resolve(path);
	return { ...route, name: route.name ?? undefined };
}

describe('router scroll behavior', () => {
	beforeEach(() => {
		vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
		prepareArticleListScroll(false);
		setPageViewPending(false);
	});

	afterEach(() => vi.restoreAllMocks());

	it.each([
		['/articles', '/articles?keyword=Vue'],
		['/articles?keyword=Vue', '/articles?keyword=Spring'],
		['/articles', '/articles?categoryId=21001'],
		['/articles', '/articles?tagIds=30001&tagIds=30002'],
		['/articles?keyword=Vue&categoryId=21001&tagIds=30001', '/articles'],
		['/articles?keyword=Vue#results', '/articles?keyword=Spring#results'],
		['/archives?mode=time&year=2026', '/archives?mode=category&categoryId=21001'],
		['/archives?mode=category&categoryId=21001', '/archives?mode=tag&tagId=30001'],
	])('preserves scrolling when filters change from %s to %s', (from, to) => {
		expect(scrollBehavior(resolveRoute(to), resolveRoute(from), null)).toBe(false);
	});

	it.each([
		['/articles?keyword=Vue', '/'],
		['/articles/40001', '/articles/40002'],
	])('scrolls to the top for navigation from %s to %s', (from, to) => {
		expect(scrollBehavior(resolveRoute(to), resolveRoute(from), null)).toEqual({
			left: 0,
			top: 0,
		});
	});

	it.each([
		['/', '/articles?keyword=Vue'],
		['/articles?keyword=Vue', '/articles?keyword=Vue#results'],
	])(
		'waits for the list before scrolling a normal navigation from %s to %s',
		async (from, to) => {
			markArticleListReady(to);

			await expect(scrollBehavior(resolveRoute(to), resolveRoute(from), null)).resolves.toBe(
				false,
			);
			expect(window.scrollTo).toHaveBeenCalledWith({ left: 0, top: 0, behavior: 'instant' });
		},
	);

	it('restores the saved document position when returning from an article', async () => {
		markArticleListReady('/articles?keyword=Vue');

		await scrollBehavior(
			resolveRoute('/articles?keyword=Vue'),
			resolveRoute('/articles/40001'),
			{
				left: 0,
				top: 1800,
			},
		);

		expect(window.scrollTo).toHaveBeenCalledWith({ left: 0, top: 1800, behavior: 'instant' });
	});
});
