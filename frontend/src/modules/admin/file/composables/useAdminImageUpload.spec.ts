import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ElMessage } from 'element-plus';
import { uploadAdminImage } from '../api/adminFileApi';
import { ADMIN_IMAGE_UPLOAD_SCENE } from '../types/adminFile';
import { useAdminImageUpload } from './useAdminImageUpload';

vi.mock('../api/adminFileApi', () => ({
	uploadAdminImage: vi.fn(),
}));

vi.mock('element-plus', () => ({
	ElMessage: {
		warning: vi.fn(),
	},
}));

describe('useAdminImageUpload', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('uploads a valid image and exposes the pending state', async () => {
		let resolveUpload!: (value: {
			url: string;
			originalName: string;
			contentType: string;
			size: number;
		}) => void;
		vi.mocked(uploadAdminImage).mockReturnValue(
			new Promise((resolve) => {
				resolveUpload = resolve;
			}),
		);
		const file = new File(['cover'], 'cover.png', { type: 'image/png' });
		const { imageUploading, handleUploadAdminImage } = useAdminImageUpload();

		const uploadPromise = handleUploadAdminImage(ADMIN_IMAGE_UPLOAD_SCENE.ARTICLE_COVER, file);
		expect(imageUploading.value).toBe(true);

		resolveUpload({
			url: 'https://img.example.com/cover.png',
			originalName: 'cover.png',
			contentType: 'image/png',
			size: file.size,
		});
		const result = await uploadPromise;

		expect(result?.url).toBe('https://img.example.com/cover.png');
		expect(imageUploading.value).toBe(false);
		expect(uploadAdminImage).toHaveBeenCalledWith('ARTICLE_COVER', file);
	});

	it('rejects an unsupported file before requesting the backend', async () => {
		const file = new File(['text'], 'fake.jpg', { type: 'text/plain' });
		const { handleUploadAdminImage } = useAdminImageUpload();

		const result = await handleUploadAdminImage(ADMIN_IMAGE_UPLOAD_SCENE.ARTICLE_CONTENT, file);

		expect(result).toBeUndefined();
		expect(uploadAdminImage).not.toHaveBeenCalled();
		expect(ElMessage.warning).toHaveBeenCalledWith('仅支持 JPG、JPEG、PNG、WebP 和 GIF 图片');
	});

	it('clears the pending state when the request fails', async () => {
		vi.mocked(uploadAdminImage).mockRejectedValue(new Error('upload failed'));
		const file = new File(['cover'], 'cover.webp', { type: 'image/webp' });
		const { imageUploading, handleUploadAdminImage } = useAdminImageUpload();

		const result = await handleUploadAdminImage(ADMIN_IMAGE_UPLOAD_SCENE.PROJECT_COVER, file);

		expect(result).toBeUndefined();
		expect(imageUploading.value).toBe(false);
	});
});
