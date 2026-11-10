import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ElMessage } from 'element-plus';
import { getAboutPage, updateAboutPage } from '../api/adminAboutApi';
import { useAdminAboutPage } from './useAdminAboutPage';

vi.mock('../api/adminAboutApi', () => ({
	getAboutPage: vi.fn(),
	updateAboutPage: vi.fn(),
}));

vi.mock('element-plus', () => ({
	ElMessage: {
		warning: vi.fn(),
		success: vi.fn(),
	},
}));

function aboutPage(contentMd = '# 关于本站') {
	return {
		exists: true,
		contentMd,
		contentHtml: '<h1>关于本站</h1>',
		contentText: '关于本站',
		updatedBy: { id: 10001, username: 'admin' },
		updatedAt: '2026-09-26T10:00:00Z',
	};
}

describe('useAdminAboutPage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('loads the editor content and tracks unsaved changes', async () => {
		vi.mocked(getAboutPage).mockResolvedValue(aboutPage());
		const page = useAdminAboutPage();

		await page.getAdminAboutPage();
		expect(page.contentMd.value).toBe('# 关于本站');
		expect(page.hasUnsavedChanges.value).toBe(false);

		page.contentMd.value = '# 新内容';
		expect(page.hasUnsavedChanges.value).toBe(true);
	});

	it('trims and saves changed content', async () => {
		vi.mocked(getAboutPage).mockResolvedValue(aboutPage());
		vi.mocked(updateAboutPage).mockResolvedValue(aboutPage('## 新内容'));
		const page = useAdminAboutPage();
		await page.getAdminAboutPage();
		page.contentMd.value = '  ## 新内容  ';

		await expect(page.saveAboutPage()).resolves.toBe(true);
		expect(updateAboutPage).toHaveBeenCalledWith({ contentMd: '## 新内容' });
		expect(page.contentMd.value).toBe('## 新内容');
		expect(page.hasUnsavedChanges.value).toBe(false);
		expect(ElMessage.success).toHaveBeenCalledWith('关于页保存成功');
	});

	it('rejects blank content before requesting the backend', async () => {
		const page = useAdminAboutPage();
		page.contentMd.value = '   ';

		await expect(page.saveAboutPage()).resolves.toBe(false);
		expect(updateAboutPage).not.toHaveBeenCalled();
		expect(ElMessage.warning).toHaveBeenCalledWith('关于页内容不能为空');
	});
});
