import { ref } from 'vue';
import { getAboutPage } from '../api/aboutApi';
import type { PublicAboutPageData } from '../types/about';

export function useAboutPage() {
	const aboutPage = ref<PublicAboutPageData | null>(null);
	const loading = ref(false);
	const loadFailed = ref(false);

	async function getAboutPageContent() {
		if (loading.value) return;

		loading.value = true;
		loadFailed.value = false;
		try {
			aboutPage.value = await getAboutPage();
		} catch {
			// 请求错误由统一响应拦截器提示。
			loadFailed.value = true;
		} finally {
			loading.value = false;
		}
	}

	return {
		aboutPage,
		loading,
		loadFailed,
		getAboutPageContent,
	};
}
