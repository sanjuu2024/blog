import { CATEGORY_LEVEL, type AdminCategoryItem } from '../types/adminCategory';
import { listCategories } from '../api/adminCategoryApi';

export function useAdminCategoryOptions() {
	// 一级分类下拉选项
	const parentCategoryOptions = ref<AdminCategoryItem[]>([]);

	// 获取一级分类下拉选项（即所有的一级分类列表）
	async function getParentCategoryOptions() {
		try {
			parentCategoryOptions.value = await listCategories({ level: CATEGORY_LEVEL.FIRST });
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	return {
		parentCategoryOptions,
		getParentCategoryOptions,
	};
}
