import { beforeEach, describe, expect, it, vi } from 'vitest';
import { listArticles } from '../api/adminArticleApi';
import { ARTICLE_STATUS } from '../types/adminArticle';
import { useAdminArticleList } from './useAdminArticleList';

vi.mock('../api/adminArticleApi', () => ({
	listArticles: vi.fn(),
}));

describe('useAdminArticleList', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(listArticles).mockResolvedValue({
			records: [],
			pageNum: 1,
			pageSize: 7,
			total: 0,
			totalPages: 0,
			hasNext: false,
		});
	});

	it('trims and converts article list filters', () => {
		const list = useAdminArticleList();
		Object.assign(list.filterForm, {
			title: '  Spring  ',
			categoryId: 21001,
			status: ARTICLE_STATUS.PUBLISHED,
			isTop: true,
		});
		list.pageParams.pageNum = 2;

		expect(list.buildArticleListQuery()).toEqual({
			pageNum: 2,
			pageSize: 7,
			title: 'Spring',
			categoryId: 21001,
			status: ARTICLE_STATUS.PUBLISHED,
			isTop: true,
		});
	});

	it('loads records and pagination metadata', async () => {
		vi.mocked(listArticles).mockResolvedValue({
			records: [
				{
					id: 40001,
					title: '测试文章',
					summary: '',
					status: ARTICLE_STATUS.DRAFT,
					isTop: false,
					coverUrl: '',
					publishedAt: null,
					updatedAt: '2026-09-01T10:00:00+08:00',
					category: {
						id: 21001,
						name: 'Java',
						level: 2,
						parent: { id: 20001, name: '技术' },
					},
				},
			],
			pageNum: 2,
			pageSize: 20,
			total: 21,
			totalPages: 2,
			hasNext: false,
		});
		const list = useAdminArticleList();

		await list.getArticleList();

		expect(list.articleList.value).toHaveLength(1);
		expect(list.pageParams).toMatchObject({ pageNum: 2, pageSize: 20 });
		expect(list.pageMeta).toEqual({ total: 21, totalPages: 2, hasNext: false });
	});

	it('resets filters and requests the default query', async () => {
		const list = useAdminArticleList();
		Object.assign(list.filterForm, {
			title: 'Spring',
			categoryId: 21001,
			status: ARTICLE_STATUS.PUBLISHED,
			isTop: true,
		});

		list.resetFilterForm();
		await Promise.resolve();

		expect(list.filterForm).toEqual({
			title: '',
			categoryId: undefined,
			status: 'ALL',
			isTop: 'ALL',
		});
		expect(listArticles).toHaveBeenCalledWith(
			expect.objectContaining({ title: undefined, status: undefined, isTop: undefined }),
		);
	});
});
