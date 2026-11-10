import { ElMessage } from 'element-plus';
import { deleteArticle, updateArticleStatus } from '../api/adminArticleApi';
import { type ArticleStatus } from '../types/adminArticle';

export function useAdminArticleActions() {
	// 当前文章id
	const articleId: number | null = null;

	// 发送 删除文章 请求
	async function handleDeleteArticle(id: number | null = articleId) {
		if (!id) {
			ElMessage.warning('文章ID不能为空');
			return;
		}

		try {
			const data = await deleteArticle(id);
			ElMessage.success('文章删除成功');
			return data;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	// 发送 修改文章状态 请求
	async function handleUpdateArticleStatus(id: number | null = articleId, status: ArticleStatus) {
		if (!id) {
			ElMessage.warning('文章ID不能为空');
			return;
		}

		try {
			const data = await updateArticleStatus(id, { status });
			ElMessage.success('文章状态更新成功');
			return data;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	return {
		handleDeleteArticle,
		handleUpdateArticleStatus,
	};
}
