import { getEnabledTags } from '../api/tagApi';
import type { PublicTagItem } from '../types/tag';

export function useEnabledTagList() {
	// 启用的标签列表
	const enabledTagList = ref<PublicTagItem[]>([]);

	// 标签列表加载状态
	const loading = ref(false);

	// 是否加载失败；具体错误提示由 request 响应拦截器统一处理
	const loadFailed = ref(false);

	// 是否已加载完毕
	const loaded = ref(false);

	// 发送 获取启用标签列表 请求
	async function getEnabledTagList() {
		if (loading.value) return;

		loading.value = true;
		loadFailed.value = false;

		try {
			const data = await getEnabledTags();
			enabledTagList.value = data;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
			loadFailed.value = true;
		} finally {
			loaded.value = true;
			loading.value = false;
		}
	}

	return {
		enabledTagList,
		loading,
		loadFailed,
		loaded,
		getEnabledTagList,
	};
}
