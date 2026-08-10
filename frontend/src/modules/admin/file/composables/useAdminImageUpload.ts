import { ElMessage } from 'element-plus';
import { computed, ref } from 'vue';
import { ADMIN_IMAGE_MAX_SIZE, validateImageFile } from '@/modules/file/utils/image';
import { uploadAdminImage } from '../api/adminFileApi';
import type { AdminImageUploadScene } from '../types/adminFile';

export function useAdminImageUpload() {
	// Markdown 编辑器可能同时上传多张图片，使用计数避免任一请求完成后过早关闭 loading。
	const pendingCount = ref(0);
	const imageUploading = computed(() => pendingCount.value > 0);

	// 先在浏览器中检查图片类型和大小，通过后再按照指定场景上传图片
	// 上传成功时返回后端生成的图片信息，校验或请求失败时返回 undefined
	// 请求错误提示由 Axios 响应拦截器统一处理，因此这里不再重复弹出错误消息
	async function handleUploadAdminImage(scene: AdminImageUploadScene, file: File) {
		const validationMessage = validateImageFile(file, ADMIN_IMAGE_MAX_SIZE);
		if (validationMessage) {
			ElMessage.warning(validationMessage);
			return;
		}

		pendingCount.value += 1;
		try {
			return await uploadAdminImage(scene, file);
		} catch {
			return;
		} finally {
			pendingCount.value -= 1;
		}
	}

	return {
		imageUploading,
		handleUploadAdminImage,
	};
}
