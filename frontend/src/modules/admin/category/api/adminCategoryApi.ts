import request from '@/utils/request';
import type {
	AdminCategoryListData,
	AdminCategoryListQuery,
	AdminCategoryListResponse,
	CategoryUpsertRequest,
	CreatedCategoryData,
	CreatedCategoryResponse,
	DeleteCategoryResponse,
	UpdatedCategoryData,
	UpdatedCategoryResponse,
} from '../types/adminCategory';

const ADMIN_CATEGORY_API = {
	listCategories: 'admin/categories',
	createCategory: 'admin/categories',
	updateCategory: (categoryId: number) => `admin/categories/${categoryId}`,
	deleteCategory: (categoryId: number) => `admin/categories/${categoryId}`,
} as const;

// 获取分类列表接口
export const listCategories = (params?: AdminCategoryListQuery): Promise<AdminCategoryListData> => {
	// 🔺注意 params 是 query 参数，所以是作为配置传过去的，request.get() 的第二个参数就是 Axios 请求配置对象，所以把 params 用 {params:params} 传进去。
	// 🔺request.post<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data)
	return request.get<AdminCategoryListResponse, AdminCategoryListData, AdminCategoryListQuery>(
		ADMIN_CATEGORY_API.listCategories,
		{ params },
	);
};

// 创建分类接口
export const createCategory = (data: CategoryUpsertRequest): Promise<CreatedCategoryData> => {
	return request.post<CreatedCategoryResponse, CreatedCategoryData, CategoryUpsertRequest>(
		ADMIN_CATEGORY_API.createCategory,
		data,
	);
};

// 更新分类接口
export const updateCategory = (
	categoryId: number,
	data: CategoryUpsertRequest,
): Promise<UpdatedCategoryData> => {
	return request.put<UpdatedCategoryResponse, UpdatedCategoryData, CategoryUpsertRequest>(
		ADMIN_CATEGORY_API.updateCategory(categoryId),
		data,
	);
};

// 删除分类接口
export const deleteCategory = (categoryId: number): Promise<null> => {
	return request.delete<DeleteCategoryResponse, null, null>(
		ADMIN_CATEGORY_API.deleteCategory(categoryId),
	);
};
