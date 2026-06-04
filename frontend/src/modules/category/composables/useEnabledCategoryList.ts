import { getEnabledCategories } from '../api/categoryApi';
import type { PublicCategoryItem } from '../types/category';

export function useEnabledCategoryList() {
	// 启用的分类列表
	const enabledCategoryList = ref<PublicCategoryItem[]>([]);

	// 发送 获取启用分类列表 请求
	async function getEnabledCategoryList() {
		try {
			const data = await getEnabledCategories();
			enabledCategoryList.value = data;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	return {
		enabledCategoryList,
		getEnabledCategoryList,
	};
}
