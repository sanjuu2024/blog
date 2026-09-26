import { ref } from 'vue';
import { getPrivacyPolicy } from '../api/privacyApi';
import type { PrivacyPolicyData } from '../types/privacy';

export function usePrivacyPolicy() {
	const privacyPolicy = ref<PrivacyPolicyData | null>(null);
	const loading = ref(false);
	const loadFailed = ref(false);

	async function getPrivacyPolicyContent() {
		if (loading.value) return;

		loading.value = true;
		loadFailed.value = false;
		try {
			privacyPolicy.value = await getPrivacyPolicy();
		} catch {
			// 请求错误由统一响应拦截器提示。
			loadFailed.value = true;
		} finally {
			loading.value = false;
		}
	}

	return {
		privacyPolicy,
		loading,
		loadFailed,
		getPrivacyPolicyContent,
	};
}
