import { ElMessage } from 'element-plus';
import { ARTICLE_STATUS, type ArticleUpsertRequest } from '../types/adminArticle';
import { createArticle, updateArticle } from '../api/adminArticleApi';

export type ArticleUpsertRequestFilter = Omit<ArticleUpsertRequest, 'categoryId'> & {
	categoryId: number | null;
};

export function useAdminArticleForm() {
	// 创建 / 更新文章 按钮的防重复点击
	const submitting = ref(false);

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

	// 表单规则
	const rules = {
		title: [
			{
				required: true,
				trigger: 'blur',
				min: 1,
				max: 200,
				validator: (
					_rule: unknown,
					value: string | undefined,
					callback: (error?: Error) => void,
				) => {
					const title = value?.trim() ?? '';
					if (title.length < 1 || title.length > 200) {
						return callback(new Error('文章标题长度必须在 1-200 字符之间'));
					}

					callback();
				},
			},
		],
		coverUrl: [
			{
				trigger: 'blur',
				max: 500,
				validator: (
					_rule: unknown,
					value: string | undefined,
					callback: (error?: Error) => void,
				) => {
					const coverUrl = value?.trim() ?? '';
					if (coverUrl.length > 500) {
						return callback(new Error('文章封面链接长度必须小于 500 字符'));
					}

					callback();
				},
			},
		],
		summary: [
			{
				max: 500,
				validator: (
					_rule: unknown,
					value: string | undefined,
					callback: (error?: Error) => void,
				) => {
					const summary = value?.trim() ?? '';
					if (summary.length > 500) {
						return callback(new Error('文章摘要长度不能超过 500 字符'));
					}

					callback();
				},
			},
		],
		categoryId: [
			{
				required: true,
				trigger: 'change',
				message: '请选择文章分类',
			},
		],
		contentMd: [
			{
				required: true,
				trigger: 'blur',
				validator: (
					_rule: unknown,
					value: string | undefined,
					callback: (error?: Error) => void,
				) => {
					if (!value?.trim()) {
						return callback(new Error('请输入文章正文'));
					}

					callback();
				},
			},
		],
	};

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
		if (submitting.value) {
			return false;
		}

		submitting.value = true;

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
		} finally {
			submitting.value = false;
		}
	}

	// 发送 更新文章 请求
	async function handleUpdateArticle(articleId: number) {
		if (submitting.value) {
			return false;
		}

		submitting.value = true;

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
		} finally {
			submitting.value = false;
		}
	}

	return {
		submitting,
		initUpsertRequest,
		upsertRequest,
		rules,
		handleCreateArticle,
		handleUpdateArticle,
	};
}
