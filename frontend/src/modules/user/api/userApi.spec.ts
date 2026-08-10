import { beforeEach, describe, expect, it, vi } from 'vitest';
import request from '@/utils/request';
import { updateCurrentUserAvatar } from './userApi';

vi.mock('@/utils/request', () => ({
	default: {
		put: vi.fn(),
	},
}));

describe('user API contracts', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('uploads the current user avatar as multipart form data', () => {
		const file = new File(['avatar'], 'avatar.jpg', { type: 'image/jpeg' });

		updateCurrentUserAvatar(file);

		expect(request.put).toHaveBeenCalledWith('users/me/avatar', expect.any(FormData), {
			timeout: 30000,
		});
		const formData = vi.mocked(request.put).mock.calls[0]?.[1] as FormData;
		expect(formData.get('file')).toBe(file);
	});
});
