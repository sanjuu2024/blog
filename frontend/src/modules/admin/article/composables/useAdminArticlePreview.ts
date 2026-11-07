import { onScopeDispose, ref, watch, type Ref } from 'vue';
import { previewArticle } from '../api/adminArticleApi';
import type { ArticlePreviewSnapshot } from '../types/adminArticle';
import type { ArticleUpsertRequestFilter } from './useAdminArticleForm';
import type { AdminCategoryItem } from '../../category/types/adminCategory';
import type { AdminTagItem } from '../../tag/types/adminTag';
import { TAG_STATUS } from '@/modules/tag/constants/tag';

export function useAdminArticlePreview(
	form: ArticleUpsertRequestFilter,
	categories: Ref<AdminCategoryItem[]>,
	tags: Ref<AdminTagItem[]>,
) {
	const visible = ref(false);
	const loading = ref(false);
	const failed = ref(false);
	const article = ref<ArticlePreviewSnapshot | null>(null);
	let requestVersion = 0;

	// 关闭或离开后忽略旧响应，避免再次打开时被前一次结果覆盖。
	watch(
		visible,
		(value) => {
			if (!value) {
				requestVersion++;
				loading.value = false;
			}
		},
		{ flush: 'sync' },
	);
	onScopeDispose(() => requestVersion++);

	async function openPreview() {
		const version = ++requestVersion;
		const contentMd = form.contentMd;
		let categoryPath = '';
		for (const parent of categories.value) {
			const child = parent.children.find((item) => item.id === form.categoryId);
			if (child) categoryPath = `${parent.name} / ${child.name}`;
		}
		// 元数据与正文使用点击时的同一份快照，不受请求期间表单变化影响。
		article.value = {
			title: form.title,
			summary: form.summary || '',
			coverUrl: form.coverUrl || '',
			categoryPath,
			tags: tags.value
				.filter((tag) => tag.status === TAG_STATUS.ENABLED && form.tagIds?.includes(tag.id))
				.map(({ id, name }) => ({ id, name })),
			contentHtml: '',
		};
		visible.value = true;
		loading.value = true;
		failed.value = false;
		try {
			const result = await previewArticle(contentMd);
			if (version === requestVersion && visible.value)
				article.value.contentHtml = result.contentHtml;
		} catch {
			if (version === requestVersion && visible.value) failed.value = true;
			// 请求错误由统一拦截器提示，弹窗保留明确的失败与重试入口。
		} finally {
			if (version === requestVersion) loading.value = false;
		}
	}

	return { visible, loading, failed, article, openPreview };
}
