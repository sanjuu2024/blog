import request from '@/utils/request';
import type {
	AdminImageUploadScene,
	UploadedImageData,
	UploadedImageResponse,
} from '../types/adminFile';

const ADMIN_FILE_API = {
	uploadImage: 'admin/files/images',
} as const;

// 上传后台文章封面、正文图片或项目封面
// scene 用于告知后端图片的使用场景，file 是用户通过文件选择器选中的原始文件
export const uploadAdminImage = (
	scene: AdminImageUploadScene,
	file: File,
): Promise<UploadedImageData> => {
	// 文件不能像普通 JSON 字段一样直接发送，需要用 FormData 组装成 multipart/form-data 请求
	// 字段名 file 和 scene 必须与后端上传接口接收的参数名保持一致
	const formData = new FormData();
	formData.append('file', file);
	formData.append('scene', scene);

	return request.post<UploadedImageResponse, UploadedImageData, FormData>(
		ADMIN_FILE_API.uploadImage,
		formData,
		{ timeout: 30000 },
	);
};
