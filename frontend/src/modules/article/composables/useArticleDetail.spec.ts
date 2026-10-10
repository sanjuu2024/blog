import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getArticleDetails, likeArticle, unlikeArticle } from '../api/articleApi';
import type { ArticleLikeData, PublicArticleDetailData } from '../types/article';
import { useArticleDetail } from './useArticleDetail';

vi.mock('../api/articleApi', () => ({
	getArticleDetails: vi.fn(),
	likeArticle: vi.fn(),
	unlikeArticle: vi.fn(),
}));

vi.mock('element-plus', () => ({
	ElMessage: { error: vi.fn() },
}));

function article(overrides: Partial<PublicArticleDetailData> = {}): PublicArticleDetailData {
	return {
		seo: {
			title: '测试文章 - 青禾边',
			description: '文章摘要',
			canonicalUrl: 'https://blog.example.com/articles/40001',
			imageUrl: null,
			type: 'article',
			publishedAt: null,
			updatedAt: null,
		},
		id: 40001,
		title: '测试文章',
		summary: '',
		contentHtml: '<p>正文</p>',
		coverUrl: '',
		isTop: false,
		allowComment: true,
		viewCount: 10,
		commentCount: 0,
		likeCount: 2,
		liked: false,
		publishedAt: '2026-09-01T10:00:00+08:00',
		updatedAt: '2026-09-01T10:00:00+08:00',
		category: {
			id: 21001,
			name: 'Java',
			level: 2,
			parent: { id: 20001, name: '技术' },
		},
		tags: [],
		author: { id: 10001, username: 'admin', nickname: '管理员', avatarUrl: '', bio: '' },
		...overrides,
	};
}

describe('useArticleDetail article likes', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('updates the button and count only after a successful like response', async () => {
		vi.mocked(getArticleDetails).mockResolvedValue(article());
		let resolveLike!: (result: ArticleLikeData) => void;
		vi.mocked(likeArticle).mockReturnValue(
			new Promise((resolve) => {
				resolveLike = resolve;
			}),
		);
		const detail = useArticleDetail();
		await detail.getArticleDetail(40001);
		expect(document.title).toBe('测试文章 - 青禾边');
		expect(document.querySelector('link[rel=canonical]')?.getAttribute('href')).toBe(
			'https://blog.example.com/articles/40001',
		);

		const firstClick = detail.toggleArticleLike();
		const secondClick = detail.toggleArticleLike();
		expect(likeArticle).toHaveBeenCalledExactlyOnceWith(40001);
		expect(detail.article.value?.liked).toBe(false);
		expect(detail.article.value?.likeCount).toBe(2);

		resolveLike({ liked: true, likeCount: 3 });
		await Promise.all([firstClick, secondClick]);
		expect(detail.article.value?.liked).toBe(true);
		expect(detail.article.value?.likeCount).toBe(3);
	});

	it('uses the unlike endpoint when already liked', async () => {
		vi.mocked(getArticleDetails).mockResolvedValue(article({ liked: true, likeCount: 3 }));
		vi.mocked(unlikeArticle).mockResolvedValue({ liked: false, likeCount: 2 });
		const detail = useArticleDetail();
		await detail.getArticleDetail(40001);

		await detail.toggleArticleLike();

		expect(unlikeArticle).toHaveBeenCalledExactlyOnceWith(40001);
		expect(detail.article.value?.liked).toBe(false);
		expect(detail.article.value?.likeCount).toBe(2);
	});

	it('keeps the current state when a like request fails', async () => {
		vi.mocked(getArticleDetails).mockResolvedValue(article());
		vi.mocked(likeArticle).mockRejectedValue(new Error('request failed'));
		const detail = useArticleDetail();
		await detail.getArticleDetail(40001);

		await detail.toggleArticleLike();

		expect(detail.article.value?.liked).toBe(false);
		expect(detail.article.value?.likeCount).toBe(2);
	});

	it('does not apply an old article response after navigation', async () => {
		vi.mocked(getArticleDetails)
			.mockResolvedValueOnce(article())
			.mockResolvedValueOnce(article({ id: 40002, likeCount: 5 }));
		let resolveLike!: (result: ArticleLikeData) => void;
		vi.mocked(likeArticle).mockReturnValue(
			new Promise((resolve) => {
				resolveLike = resolve;
			}),
		);
		const detail = useArticleDetail();
		await detail.getArticleDetail(40001);
		const pending = detail.toggleArticleLike();
		await detail.getArticleDetail(40002);

		resolveLike({ liked: true, likeCount: 3 });
		await pending;

		expect(detail.article.value?.id).toBe(40002);
		expect(detail.article.value?.liked).toBe(false);
		expect(detail.article.value?.likeCount).toBe(5);
	});

	it('ignores a slower article detail and its SEO after requesting a newer article', async () => {
		let resolveOld!: (value: PublicArticleDetailData) => void;
		vi.mocked(getArticleDetails).mockImplementationOnce(
			() =>
				new Promise((resolve) => {
					resolveOld = resolve;
				}),
		);
		const nextArticle = article({ id: 40002 });
		nextArticle.seo = {
			...nextArticle.seo,
			title: '新文章',
			canonicalUrl: 'https://blog.example.com/articles/40002',
		};
		vi.mocked(getArticleDetails).mockResolvedValueOnce(nextArticle);
		const detail = useArticleDetail();
		const oldRequest = detail.getArticleDetail(40001);
		await detail.getArticleDetail(40002);
		resolveOld(article());
		await oldRequest;

		expect(detail.article.value?.id).toBe(40002);
		expect(document.title).toBe('新文章');
		expect(document.querySelector('link[rel=canonical]')?.getAttribute('href')).toBe(
			nextArticle.seo.canonicalUrl,
		);
	});

	it('clears server article tags and uses a failure title when detail loading fails', async () => {
		vi.mocked(getArticleDetails)
			.mockResolvedValueOnce(article())
			.mockRejectedValueOnce(new Error('network error'));
		const detail = useArticleDetail();
		await detail.getArticleDetail(40001);
		await detail.getArticleDetail(40002);

		expect(document.querySelector('link[rel=canonical]')).toBeNull();
		expect(document.querySelector('meta[name=robots]')?.getAttribute('content')).toBe(
			'noindex, follow',
		);
		expect(document.title).toContain('文章加载失败');
	});
});
