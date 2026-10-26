import { beforeEach, describe, expect, it, vi } from 'vitest';
import request from '@/utils/request';
import {
	changeCurrentUserEmail,
	deleteCurrentUser,
	sendEmailChangeCode,
	updateCurrentUserProfile,
} from './userApi';

vi.mock('@/utils/request', () => ({
	default: {
		delete: vi.fn(),
		post: vi.fn(),
		put: vi.fn(),
	},
}));

describe('user API contracts', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('submits profile fields and the optional avatar in one multipart request', () => {
		const file = new File(['avatar'], 'avatar.webp', { type: 'image/webp' });

		updateCurrentUserProfile({ nickname: 'Alice', bio: 'Bio' }, file);

		expect(request.put).toHaveBeenCalledWith('users/me/profile', expect.any(FormData), {
			timeout: 30000,
		});
		const formData = vi.mocked(request.put).mock.calls[0]?.[1] as FormData;
		expect(formData.get('nickname')).toBe('Alice');
		expect(formData.get('bio')).toBe('Bio');
		expect(formData.get('avatar')).toBe(file);
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

	it('submits the current user deletion request', () => {
		deleteCurrentUser({ password: 'Password_1' });

		expect(request.delete).toHaveBeenCalledWith('users/me', {
			data: { password: 'Password_1' },
		});
	});
});
