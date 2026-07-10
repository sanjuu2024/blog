import { getEnabledCategories } from '../api/categoryApi';
import type { PublicCategoryItem } from '../types/category';

export function useEnabledCategoryList() {
	// 启用的分类列表
	const enabledCategoryList = ref<PublicCategoryItem[]>([]);

	// 分类列表加载状态
	const loading = ref(false);

	// 是否加载失败；具体错误提示由 request 响应拦截器统一处理
	const loadFailed = ref(false);

	// 是否已加载完毕
	const loaded = ref(false);

	// 发送 获取启用分类列表 请求
	async function getEnabledCategoryList() {
		if (loading.value) return;

		loading.value = true;
		loadFailed.value = false;

		try {
			const data = await getEnabledCategories();
			enabledCategoryList.value = data;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
			loadFailed.value = true;
		} finally {
			loaded.value = true;
			loading.value = false;
		}
	}

	return {
		enabledCategoryList,
		loading,
		loadFailed,
		loaded,
		getEnabledCategoryList,
	};
}
