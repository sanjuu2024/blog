import { describe, expect, it } from 'vitest';
import { ADMIN_IMAGE_MAX_SIZE, AVATAR_MAX_SIZE, validateImageFile } from './image';

describe('validateImageFile', () => {
	it('accepts all documented image MIME types', () => {
		for (const type of ['image/jpeg', 'image/png', 'image/webp', 'image/gif']) {
			const file = new File(['image'], 'image', { type });
			expect(validateImageFile(file, AVATAR_MAX_SIZE)).toBeNull();
		}
	});

	it('rejects unsupported MIME types', () => {
		const file = new File(['svg'], 'image.svg', { type: 'image/svg+xml' });

		expect(validateImageFile(file, ADMIN_IMAGE_MAX_SIZE)).toBe(
			'仅支持 JPG、JPEG、PNG、WebP 和 GIF 图片',
		);
	});

	it('uses the scene-specific size limit', () => {
		const oversizedAvatar = new File([new Uint8Array(AVATAR_MAX_SIZE + 1)], 'avatar.jpg', {
			type: 'image/jpeg',
		});

		expect(validateImageFile(oversizedAvatar, AVATAR_MAX_SIZE)).toBe('图片大小不能超过 2 MB');
	});
});
