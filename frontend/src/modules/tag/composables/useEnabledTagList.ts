import { getEnabledTags } from '../api/tagApi';
import type { PublicTagItem } from '../types/tag';

export function useEnabledTagList() {
	// 启用的标签列表
	const enabledTagList = ref<PublicTagItem[]>([]);

	// 发送 获取启用标签列表 请求
	async function getEnabledTagList() {
		try {
			const data = await getEnabledTags();
			enabledTagList.value = data;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	return {
		enabledTagList,
		getEnabledTagList,
	};
}
