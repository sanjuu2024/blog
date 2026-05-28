import { getArticleDetails } from '../api/adminArticleApi';
import { ARTICLE_STATUS, type AdminArticleDetailData } from '../types/adminArticle';

export function useAdminArticleDetail() {
	// 文章数据初始值
	const initArticleDetail: AdminArticleDetailData = {
		id: 0,
		title: '',
		summary: '',
		contentMd: '',
		contentHtml: '',
		contentText: '',
		coverUrl: '',
		status: ARTICLE_STATUS.DRAFT,
		isTop: false,
		categoryId: 0,
		tagIds: [],
		allowComment: true,
		publishedAt: '',
		createdAt: '',
		updatedAt: '',
	};

	// 文章数据
	const articleDetail = reactive<AdminArticleDetailData>({ ...initArticleDetail });

	// 发送 获取文章详情 请求
	async function handleGetArticleDetails(articleId: number) {
		try {
			const data: AdminArticleDetailData = await getArticleDetails(articleId);
			Object.assign(articleDetail, data);
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
			return null;
		}
	}

	return {
		articleDetail,
		handleGetArticleDetails,
	};
}
