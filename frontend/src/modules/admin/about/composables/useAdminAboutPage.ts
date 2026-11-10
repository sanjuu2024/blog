import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import 'element-plus/es/components/message/style/css';
import { getAboutPage, updateAboutPage } from '../api/adminAboutApi';
import type { AdminAboutPageData } from '../types/adminAbout';

const ABOUT_CONTENT_MAX_LENGTH = 100000;

export function useAdminAboutPage() {
	const aboutPage = ref<AdminAboutPageData | null>(null);
	const contentMd = ref('');
	const originalContentMd = ref('');
	const loading = ref(false);
	const loadFailed = ref(false);
	const submitting = ref(false);

	const hasUnsavedChanges = computed(() => contentMd.value !== originalContentMd.value);

	async function getAdminAboutPage() {
		if (loading.value) return false;

		loading.value = true;
		loadFailed.value = false;
		try {
			const data = await getAboutPage();
			aboutPage.value = data;
			contentMd.value = data.contentMd;
			originalContentMd.value = data.contentMd;
			return true;
		} catch {
			// 请求错误由统一响应拦截器提示。
			loadFailed.value = true;
			return false;
		} finally {
			loading.value = false;
		}
	}

	async function saveAboutPage() {
		const normalizedContentMd = contentMd.value.trim();
		if (!normalizedContentMd) {
			ElMessage.warning('关于页内容不能为空');
			return false;
		}
		if (normalizedContentMd.length > ABOUT_CONTENT_MAX_LENGTH) {
			ElMessage.warning('关于页内容不能超过 100000 个字符');
			return false;
		}
		if (submitting.value) return false;

		submitting.value = true;
		try {
			const data = await updateAboutPage({ contentMd: normalizedContentMd });
			aboutPage.value = data;
			contentMd.value = data.contentMd;
			originalContentMd.value = data.contentMd;
			ElMessage.success('关于页保存成功');
			return true;
		} catch {
			// 请求错误由统一响应拦截器提示。
			return false;
		} finally {
			submitting.value = false;
		}
	}

	return {
		aboutPage,
		contentMd,
		loading,
		loadFailed,
		submitting,
		hasUnsavedChanges,
		getAdminAboutPage,
		saveAboutPage,
	};
}
