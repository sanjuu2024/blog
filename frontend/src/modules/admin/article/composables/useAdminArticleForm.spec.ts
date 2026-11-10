import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createArticle, updateArticle } from '../api/adminArticleApi';
import { ARTICLE_STATUS } from '../types/adminArticle';
import { useAdminArticleForm } from './useAdminArticleForm';

const message = vi.hoisted(() => ({ success: vi.fn(), warning: vi.fn() }));

vi.mock('element-plus', () => ({
	ElMessage: message,
}));

vi.mock('../api/adminArticleApi', () => ({
	createArticle: vi.fn(),
	updateArticle: vi.fn(),
}));

function fillValidForm(form: ReturnType<typeof useAdminArticleForm>) {
	Object.assign(form.upsertRequest, {
		title: '测试文章',
		summary: '文章摘要',
		contentMd: '# 正文',
		categoryId: 21001,
		tagIds: [30001],
		coverUrl: 'https://img.example.com/cover.png',
		isTop: true,
		status: ARTICLE_STATUS.PUBLISHED,
		allowComment: true,
	});
}

describe('useAdminArticleForm', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(createArticle).mockResolvedValue({
			id: 40001,
			status: ARTICLE_STATUS.PUBLISHED,
			publishedAt: '2026-09-01T10:00:00+08:00',
			createdAt: '2026-09-01T10:00:00+08:00',
		});
		vi.mocked(updateArticle).mockResolvedValue({
			id: 40001,
			status: ARTICLE_STATUS.PUBLISHED,
			publishedAt: '2026-09-01T10:00:00+08:00',
			updatedAt: '2026-09-01T11:00:00+08:00',
		});
	});

	it('rejects submission when no category is selected', async () => {
		const form = useAdminArticleForm();

		await expect(form.handleCreateArticle()).resolves.toBe(false);

		expect(createArticle).not.toHaveBeenCalled();
		expect(message.warning).toHaveBeenCalledWith('请选择文章分类');
		expect(form.submitting.value).toBe(false);
	});

	it('creates an article with the current form values', async () => {
		const form = useAdminArticleForm();
		fillValidForm(form);

		await expect(form.handleCreateArticle()).resolves.toBe(true);

		expect(createArticle).toHaveBeenCalledWith({
			title: '测试文章',
			summary: '文章摘要',
			contentMd: '# 正文',
			categoryId: 21001,
			tagIds: [30001],
			coverUrl: 'https://img.example.com/cover.png',
			isTop: true,
			status: ARTICLE_STATUS.PUBLISHED,
			allowComment: true,
		});
		expect(message.success).toHaveBeenCalledWith('文章创建成功');
		expect(form.submitting.value).toBe(false);
	});

	it('prevents duplicate creation while a request is pending', async () => {
		let resolveCreate!: () => void;
		vi.mocked(createArticle).mockReturnValue(
			new Promise((resolve) => {
				resolveCreate = () =>
					resolve({
						id: 40001,
						status: ARTICLE_STATUS.DRAFT,
						publishedAt: null,
						createdAt: '2026-09-01T10:00:00+08:00',
					});
			}),
		);
		const form = useAdminArticleForm();
		fillValidForm(form);

		const first = form.handleCreateArticle();
		await expect(form.handleCreateArticle()).resolves.toBe(false);
		expect(createArticle).toHaveBeenCalledTimes(1);

		resolveCreate();
		await expect(first).resolves.toBe(true);
	});

	it('updates an article and resets submitting after failures', async () => {
		const form = useAdminArticleForm();
		fillValidForm(form);

		await expect(form.handleUpdateArticle(40001)).resolves.toBe(true);
		expect(updateArticle).toHaveBeenCalledWith(
			40001,
			expect.objectContaining({ categoryId: 21001 }),
		);
		expect(message.success).toHaveBeenCalledWith('文章更新成功');

		vi.mocked(updateArticle).mockRejectedValueOnce(new Error('network failed'));
		await expect(form.handleUpdateArticle(40001)).resolves.toBe(false);
		expect(form.submitting.value).toBe(false);
	});
});
