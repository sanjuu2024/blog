import { ElMessage } from 'element-plus';
import { ARTICLE_STATUS, type ArticleUpsertRequest } from '../types/adminArticle';
import { createArticle, updateArticle } from '../api/adminArticleApi';

export type ArticleUpsertRequestFilter = Omit<ArticleUpsertRequest, 'categoryId'> & {
	categoryId: number | null;
};

export function useAdminArticleForm() {
	// 创建 / 更新文章的请求参数初始值
	const initUpsertRequest: ArticleUpsertRequestFilter = {
		title: '',
		summary: '',
		contentMd: '',
		categoryId: null,
		tagIds: [],
		coverUrl: '',
		isTop: false,
		status: ARTICLE_STATUS.DRAFT,
		allowComment: true,
	};

	// 创建 / 更新文章的请求参数
	const upsertRequest: ArticleUpsertRequestFilter = reactive({ ...initUpsertRequest });

	// 将表单参数转化为请求参数
	function buildUpsertRequest(
		filter: ArticleUpsertRequestFilter,
	): ArticleUpsertRequest | undefined {
		if (filter.categoryId === null) {
			ElMessage.warning('请选择文章分类');
			return;
		}

		return {
			...filter,
			categoryId: filter.categoryId!,
		};
	}

	// 发送 创建文章 请求
	async function handleCreateArticle() {
		try {
			const payload = buildUpsertRequest(upsertRequest);
			if (!payload) {
				return false;
			}

			await createArticle(payload);
			ElMessage.success('文章创建成功');

			return true;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
			return false;
		}
	}

	// 发送 更新文章 请求
	async function handleUpdateArticle(articleId: number) {
		try {
			const payload = buildUpsertRequest(upsertRequest);
			if (!payload) {
				return false;
			}

			await updateArticle(articleId, payload);
			ElMessage.success('文章更新成功');

			return true;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
			return false;
		}
	}

	return {
		upsertRequest,
		handleCreateArticle,
		handleUpdateArticle,
	};
}
