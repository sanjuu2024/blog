import { beforeEach, describe, expect, it, vi } from 'vitest';
import { deleteArticle, updateArticleStatus } from '../api/adminArticleApi';
import { ARTICLE_STATUS } from '../types/adminArticle';
import { useAdminArticleActions } from './useAdminArticleActions';

const message = vi.hoisted(() => ({ success: vi.fn(), warning: vi.fn() }));

vi.mock('element-plus', () => ({
	ElMessage: message,
}));

vi.mock('../api/adminArticleApi', () => ({
	deleteArticle: vi.fn(),
	updateArticleStatus: vi.fn(),
}));

describe('useAdminArticleActions', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(deleteArticle).mockResolvedValue(null);
		vi.mocked(updateArticleStatus).mockResolvedValue({
			id: 40001,
			status: ARTICLE_STATUS.PUBLISHED,
			updatedAt: '2026-09-01T10:00:00+08:00',
		});
	});

	it('rejects actions without an article id', async () => {
		const actions = useAdminArticleActions();

		await actions.handleDeleteArticle(null);
		await actions.handleUpdateArticleStatus(null, ARTICLE_STATUS.PUBLISHED);

		expect(deleteArticle).not.toHaveBeenCalled();
		expect(updateArticleStatus).not.toHaveBeenCalled();
		expect(message.warning).toHaveBeenCalledTimes(2);
	});

	it('deletes an article and reports success', async () => {
		const actions = useAdminArticleActions();

		await expect(actions.handleDeleteArticle(40001)).resolves.toBeNull();

		expect(deleteArticle).toHaveBeenCalledWith(40001);
		expect(message.success).toHaveBeenCalledWith('文章删除成功');
	});

	it('updates article status and returns the server result', async () => {
		const actions = useAdminArticleActions();

		await expect(
			actions.handleUpdateArticleStatus(40001, ARTICLE_STATUS.PUBLISHED),
		).resolves.toEqual(expect.objectContaining({ status: ARTICLE_STATUS.PUBLISHED }));

		expect(updateArticleStatus).toHaveBeenCalledWith(40001, {
			status: ARTICLE_STATUS.PUBLISHED,
		});
		expect(message.success).toHaveBeenCalledWith('文章状态更新成功');
	});
});
