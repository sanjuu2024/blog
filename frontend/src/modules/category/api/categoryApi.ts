import request from '@/utils/request';
import type { PublicCategoryListData, PublicCategoryListResponse } from '../types/category';

const CATEGORY_API = {
	getEnabledCategories: 'categories',
} as const;

// 获取启用分类列表接口
export const getEnabledCategories = (): Promise<PublicCategoryListData> => {
	// 🔺注意 params 是 query 参数，所以是作为配置传过去的，request.get() 的第二个参数就是 Axios 请求配置对象，所以把 params 用 {params:params} 传进去。
	// 🔺request.post<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data)
	return request.get<PublicCategoryListResponse, PublicCategoryListData, null>(
		CATEGORY_API.getEnabledCategories,
		{ params: null },
	);
};
