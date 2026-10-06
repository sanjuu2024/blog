import { beforeEach, describe, expect, it, vi } from 'vitest';
import request from '@/utils/request';
import { changeCurrentUserEmail, sendEmailChangeCode, updateCurrentUserAvatar } from './userApi';

vi.mock('@/utils/request', () => ({
	default: {
		post: vi.fn(),
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

	it('sends the email change verification code', () => {
		sendEmailChangeCode({ email: 'new@example.com' });

		expect(request.post).toHaveBeenCalledWith(
			'users/me/email-verification-codes',
			{
				email: 'new@example.com',
			},
			{ timeout: 30000 },
		);
	});

	it('submits the email change request', () => {
		const data = {
			currentPassword: 'OldPassword_123',
			newEmail: 'new@example.com',
			verificationCode: '123456',
		};

		changeCurrentUserEmail(data);

		expect(request.put).toHaveBeenCalledWith('users/me/email', data);
	});
});
