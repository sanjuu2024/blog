import { effectScope, nextTick, reactive, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { previewArticle } from '../api/adminArticleApi';
import { useAdminArticlePreview } from './useAdminArticlePreview';
import type { ArticleUpsertRequestFilter } from './useAdminArticleForm';
import type { AdminCategoryItem } from '../../category/types/adminCategory';
import type { AdminTagItem } from '../../tag/types/adminTag';

vi.mock('../api/adminArticleApi', () => ({ previewArticle: vi.fn() }));

function setup() {
	const form = reactive<ArticleUpsertRequestFilter>({
		title: '未保存标题',
		contentMd: '# 未保存正文',
		summary: '未保存摘要',
		coverUrl: '',
		categoryId: null,
		tagIds: [],
		status: 'DRAFT',
	});
	const scope = effectScope();
	const preview = scope.run(() => useAdminArticlePreview(form, ref([]), ref([])))!;
	return { form, preview, scope };
}

describe('useAdminArticlePreview', () => {
	beforeEach(() => vi.clearAllMocks());

	it('previews unsaved snapshots without requiring category or saving the form', async () => {
		let resolve!: (result: { contentHtml: string }) => void;
		vi.mocked(previewArticle).mockReturnValue(
			new Promise((done) => {
				resolve = done;
			}),
		);
		const { form, preview, scope } = setup();
		const pending = preview.openPreview();
		form.title = '请求期间的新标题';
		form.contentMd = '新的正文';
		resolve({ contentHtml: '<h1>未保存正文</h1>' });
		await pending;

		expect(previewArticle).toHaveBeenCalledExactlyOnceWith('# 未保存正文');
		expect(preview.article.value).toMatchObject({
			title: '未保存标题',
			contentHtml: '<h1>未保存正文</h1>',
			categoryPath: '',
		});
		expect(form.contentMd).toBe('新的正文');
		expect(preview.loading.value).toBe(false);
		scope.stop();
	});

	it('ignores a closed request when reopening and permits retry after failure', async () => {
		let resolveOld!: (result: { contentHtml: string }) => void;
		vi.mocked(previewArticle).mockReturnValueOnce(
			new Promise((done) => {
				resolveOld = done;
			}),
		);
		const { preview, scope } = setup();
		const old = preview.openPreview();
		preview.visible.value = false;
		await nextTick();
		expect(preview.loading.value).toBe(false);
		vi.mocked(previewArticle).mockResolvedValueOnce({ contentHtml: '<p>新预览</p>' });
		await preview.openPreview();
		resolveOld({ contentHtml: '<p>旧预览</p>' });
		await old;
		expect(preview.article.value?.contentHtml).toBe('<p>新预览</p>');
		vi.mocked(previewArticle).mockRejectedValueOnce(new Error('network error'));
		await preview.openPreview();
		expect(preview.failed.value).toBe(true);
		vi.mocked(previewArticle).mockResolvedValueOnce({ contentHtml: '<p>重试成功</p>' });
		await preview.openPreview();
		expect(preview.failed.value).toBe(false);
		expect(preview.article.value?.contentHtml).toBe('<p>重试成功</p>');
		scope.stop();
	});

	it('captures the full category path and only tags visible on the public article', async () => {
		vi.mocked(previewArticle).mockResolvedValue({ contentHtml: '<p>正文</p>' });
		const { form, scope: originalScope } = setup();
		originalScope.stop();
		form.categoryId = 2;
		form.tagIds = [3, 4];
		const categories = ref([
			{ name: '技术', children: [{ id: 2, name: 'Java' }] },
		] as AdminCategoryItem[]);
		const tags = ref([
			{ id: 3, name: '启用标签', status: 'ENABLED' },
			{ id: 4, name: '禁用标签', status: 'DISABLED' },
		] as AdminTagItem[]);
		const scope = effectScope();
		const preview = scope.run(() => useAdminArticlePreview(form, categories, tags))!;
		await preview.openPreview();
		expect(preview.article.value?.categoryPath).toBe('技术 / Java');
		expect(preview.article.value?.tags).toEqual([{ id: 3, name: '启用标签' }]);
		scope.stop();
	});
});
