import { beforeEach, describe, expect, it, vi } from 'vitest';
import { listArticles } from '../api/articleApi';
import { useArticleList } from './useArticleList';
import type { PublicArticlePageData } from '../types/article';

vi.mock('../api/articleApi', () => ({
	listArticles: vi.fn(),
}));

function articlePage(overrides: Partial<PublicArticlePageData> = {}): PublicArticlePageData {
	return {
		total: 0,
		totalPages: 0,
		pageNum: 1,
		pageSize: 10,
		hasNext: false,
		records: [],
		...overrides,
	};
}

describe('useArticleList', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('requests public articles with pagination and selected filters', async () => {
		vi.mocked(listArticles).mockResolvedValue(articlePage());
		const { filterForm, getPublishedArticles } = useArticleList();
		filterForm.keyword = 'Spring Boot';
		filterForm.categoryId = 21001;
		filterForm.tagIds = [30001, 30002];

		const result = await getPublishedArticles();

		expect(result).toBe('success');
		expect(listArticles).toHaveBeenCalledWith(
			{
				pageNum: 1,
				pageSize: 10,
				keyword: 'Spring Boot',
				categoryId: 21001,
				tagIds: [30001, 30002],
			},
			expect.objectContaining({ signal: expect.any(AbortSignal) }),
		);
	});

	it('replaces stale records when filters are reapplied', async () => {
		vi.mocked(listArticles)
			.mockResolvedValueOnce(
				articlePage({
					records: [
						{
							id: 40001,
							title: 'Old article',
							summary: '',
							coverUrl: '',
							isTop: false,
							publishedAt: '2026-05-01T10:00:00+08:00',
							viewCount: 0,
							category: {
								id: 21001,
								name: 'Java',
								level: 2,
								parent: { id: 20001, name: '技术' },
							},
							tags: [],
						},
					],
				}),
			)
			.mockResolvedValueOnce(articlePage());
		const { articles, getPublishedArticles, resetPageParamsAndGetPublishedArticles } =
			useArticleList();

		await getPublishedArticles();
		expect(articles.value).toHaveLength(1);
		await resetPageParamsAndGetPublishedArticles();

		expect(articles.value).toEqual([]);
	});

	it('stores the total after the current query loads successfully', async () => {
		vi.mocked(listArticles).mockResolvedValue(articlePage({ total: 12 }));
		const { total, hasLoaded, getPublishedArticles } = useArticleList();

		expect(hasLoaded.value).toBe(false);
		await getPublishedArticles();

		expect(total.value).toBe(12);
		expect(hasLoaded.value).toBe(true);
	});

	it('loads the next page and appends records', async () => {
		vi.mocked(listArticles).mockResolvedValue(articlePage({ pageNum: 2, hasNext: false }));
		const { pageParams, loadMoreArticles } = useArticleList();
		pageParams.hasNext = true;

		await loadMoreArticles();

		expect(pageParams.pageNum).toBe(2);
		expect(listArticles).toHaveBeenCalledWith(
			expect.objectContaining({ pageNum: 2 }),
			expect.any(Object),
		);
	});

	it('rolls page number back when loading the next page fails', async () => {
		vi.mocked(listArticles).mockRejectedValue(new Error('network failed'));
		const { pageParams, loadMoreArticles } = useArticleList();
		pageParams.hasNext = true;

		await loadMoreArticles();

		expect(pageParams.pageNum).toBe(1);
	});

	it('resets filter values to their initial state', () => {
		const { filterForm, resetFilterForm } = useArticleList();
		filterForm.keyword = 'Redis';
		filterForm.categoryId = 21001;
		filterForm.tagIds = [30001];

		resetFilterForm();

		expect(filterForm).toEqual({ keyword: '', categoryId: undefined, tagIds: [] });
	});
});
