import request from '@/utils/request';
import type {
	PublicArticlePageData,
	PublicArticleDetailData,
	PublicArticleListQuery,
	PublicArticlePageResponse,
	PublicArticleDetailResponse,
} from '../types/article';
import type { AxiosRequestConfig } from 'axios';

const ARTICLE_API = {
	listArticles: 'articles',
	getArticleDetails: (articleId: number) => `articles/${articleId}`,
} as const;

// 获取已发布文章分页列表接口
export const listArticles = (
	params?: PublicArticleListQuery,
	config?: Omit<AxiosRequestConfig, 'params'>,
): Promise<PublicArticlePageData> => {
	// 🔺注意 params 是 query 参数，所以是作为配置传过去的，request.get() 的第二个参数就是 Axios 请求配置对象，所以把 params 用 {params:params} 传进去。
	// 🔺request.post<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data)
	return request.get<PublicArticlePageResponse, PublicArticlePageData, PublicArticleListQuery>(
		ARTICLE_API.listArticles,
		{ params, ...config },
	);
};

// 获取文章详情接口
export const getArticleDetails = (articleId: number): Promise<PublicArticleDetailData> => {
	// 🔺注意 params 是 query 参数，所以是作为配置传过去的，request.get() 的第二个参数就是 Axios 请求配置对象，所以把 params 用 {params:params} 传进去。
	// 🔺request.post<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data)
	return request.get<PublicArticleDetailResponse, PublicArticleDetailData, null>(
		ARTICLE_API.getArticleDetails(articleId),
	);
};
