import request from '@/utils/request';
import type {
	AdminArticleListQuery,
	AdminArticlePageData,
	AdminArticlePageResponse,
	AdminArticleDetailData,
	AdminArticleDetailResponse,
	ArticleUpsertRequest,
	CreatedArticleData,
	CreatedArticleResponse,
	UpdatedArticleData,
	UpdatedArticleResponse,
	DeleteArticleResponse,
	UpdateArticleStatusRequest,
	UpdatedArticleStatusData,
	UpdatedArticleStatusResponse,
} from '../types/adminArticle';

const ADMIN_ARTICLE_API = {
	listArticles: 'admin/articles',
	getArticleDetails: (articleId: number) => `admin/articles/${articleId}`,
	createArticle: 'admin/articles',
	updateArticle: (articleId: number) => `admin/articles/${articleId}`,
	deleteArticle: (articleId: number) => `admin/articles/${articleId}`,
	updateArticleStatus: (articleId: number) => `admin/articles/${articleId}/status`,
} as const;

// 获取后台文章分页列表接口
export const listArticles = (params?: AdminArticleListQuery): Promise<AdminArticlePageData> => {
	// 🔺注意 params 是 query 参数，所以是作为配置传过去的，request.get() 的第二个参数就是 Axios 请求配置对象，所以把 params 用 {params:params} 传进去。
	// 🔺request.post<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data)
	return request.get<AdminArticlePageResponse, AdminArticlePageData, AdminArticleListQuery>(
		ADMIN_ARTICLE_API.listArticles,
		{ params },
	);
};

// 获取后台文章详情
export const getArticleDetails = (articleId: number): Promise<AdminArticleDetailData> => {
	// 🔺注意 params 是 query 参数，所以是作为配置传过去的，request.get() 的第二个参数就是 Axios 请求配置对象，所以把 params 用 {params:params} 传进去。
	// 🔺request.post<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data)
	return request.get<AdminArticleDetailResponse, AdminArticleDetailData, null>(
		ADMIN_ARTICLE_API.getArticleDetails(articleId),
		{ params: null },
	);
};

// 创建文章接口
export const createArticle = (data: ArticleUpsertRequest): Promise<CreatedArticleData> => {
	return request.post<CreatedArticleResponse, CreatedArticleData, ArticleUpsertRequest>(
		ADMIN_ARTICLE_API.createArticle,
		data,
	);
};

// 更新文章接口
export const updateArticle = (
	articleId: number,
	data: ArticleUpsertRequest,
): Promise<UpdatedArticleData> => {
	return request.put<UpdatedArticleResponse, UpdatedArticleData, ArticleUpsertRequest>(
		ADMIN_ARTICLE_API.updateArticle(articleId),
		data,
	);
};

// 删除文章接口
export const deleteArticle = (articleId: number): Promise<null> => {
	return request.delete<DeleteArticleResponse, null, null>(
		ADMIN_ARTICLE_API.deleteArticle(articleId),
	);
};

// 修改文章状态接口
export const updateArticleStatus = (
	articleId: number,
	data: UpdateArticleStatusRequest,
): Promise<UpdatedArticleStatusData> => {
	return request.patch<
		UpdatedArticleStatusResponse,
		UpdatedArticleStatusData,
		UpdateArticleStatusRequest
	>(ADMIN_ARTICLE_API.updateArticleStatus(articleId), data);
};
