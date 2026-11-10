export const IMAGE_ACCEPT = '.jpg,.jpeg,.png,.webp,.gif'; // 文件选择器展示的可选图片类型
export const AVATAR_MAX_SIZE = 2 * 1024 * 1024; // 头像最大 2 MB
export const ADMIN_IMAGE_MAX_SIZE = 10 * 1024 * 1024; // 后台图片最大 10 MB

const IMAGE_CONTENT_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif']);

// 在发送请求前检查图片的 MIME 类型和文件大小，避免上传明显不符合要求的文件
// maxSize 的单位为字节；校验失败时返回错误提示，通过时返回 null
// 浏览器提供的 MIME 类型可能被伪造，因此文件的真实格式仍由后端读取文件头进行校验
export function validateImageFile(file: File, maxSize: number) {
	if (!IMAGE_CONTENT_TYPES.has(file.type.toLowerCase())) {
		return '仅支持 JPG、JPEG、PNG、WebP 和 GIF 图片';
	}
	if (file.size > maxSize) {
		return `图片大小不能超过 ${maxSize / 1024 / 1024} MB`;
	}
	return null;
}
