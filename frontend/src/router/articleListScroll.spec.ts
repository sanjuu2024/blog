import { flushPromises } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
	articleListScrollPending,
	markArticleListReady,
	prepareArticleListScroll,
	restoreArticleListScroll,
	setPageViewPending,
} from './articleListScroll';

describe('article list scroll restoration', () => {
	beforeEach(() => {
		vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
		prepareArticleListScroll(true);
		setPageViewPending(false);
	});

	afterEach(() => {
		prepareArticleListScroll(false);
		vi.restoreAllMocks();
	});

	it('waits for the rendered list to be inserted before restoring', async () => {
		setPageViewPending(true);
		const restoring = restoreArticleListScroll('/articles?keyword=Vue', { left: 0, top: 2400 });
		markArticleListReady('/articles?keyword=Vue');
		await flushPromises();
		expect(window.scrollTo).not.toHaveBeenCalled();
		expect(articleListScrollPending.value).toBe(true);

		setPageViewPending(false);
		await restoring;

		expect(window.scrollTo).toHaveBeenCalledExactlyOnceWith({
			left: 0,
			top: 2400,
			behavior: 'instant',
		});
		expect(articleListScrollPending.value).toBe(false);
	});

	it('does not use readiness from a different set of filters', async () => {
		const restoring = restoreArticleListScroll('/articles?keyword=Vue', { left: 0, top: 300 });
		markArticleListReady('/articles?keyword=Spring');
		await flushPromises();
		expect(window.scrollTo).not.toHaveBeenCalled();

		markArticleListReady('/articles?keyword=Vue');
		await restoring;
		expect(window.scrollTo).toHaveBeenCalledOnce();
	});

	it('cancels an old restoration when navigating away before the list is ready', async () => {
		const restoring = restoreArticleListScroll('/articles', { left: 0, top: 3000 });
		prepareArticleListScroll(false);
		markArticleListReady('/articles');

		await restoring;

		expect(window.scrollTo).not.toHaveBeenCalled();
		expect(articleListScrollPending.value).toBe(false);
	});

	it('starts at the top if the original loaded pages are no longer cached', async () => {
		markArticleListReady('/articles', false);

		await restoreArticleListScroll('/articles', { left: 0, top: 3000 });

		expect(window.scrollTo).toHaveBeenCalledExactlyOnceWith({
			left: 0,
			top: 0,
			behavior: 'instant',
		});
	});
});
