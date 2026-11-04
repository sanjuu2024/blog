import { beforeEach, describe, expect, it, vi } from 'vitest';
import request from '@/utils/request';
import { ADMIN_IMAGE_UPLOAD_SCENE } from '../types/adminFile';
import { uploadAdminImage } from './adminFileApi';

vi.mock('@/utils/request', () => ({
	default: {
		post: vi.fn(),
	},
}));

describe('admin file API contracts', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it.each(['ARTICLE_COVER', 'ABOUT_CONTENT'] as const)(
		'uploads an admin image with scene %s',
		(scene) => {
			const file = new File(['cover'], 'cover.png', { type: 'image/png' });

			uploadAdminImage(ADMIN_IMAGE_UPLOAD_SCENE[scene], file);

			expect(request.post).toHaveBeenCalledWith('admin/files/images', expect.any(FormData), {
				timeout: 30000,
			});
			const formData = vi.mocked(request.post).mock.calls[0]?.[1] as FormData;
			expect(formData.get('file')).toBe(file);
			expect(formData.get('scene')).toBe(scene);
		},
	);
});
