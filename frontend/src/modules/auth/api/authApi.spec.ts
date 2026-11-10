import { beforeEach, describe, expect, it, vi } from 'vitest';
import request from '@/utils/request';
import { refreshToken, register, sendEmailVerificationCode } from './authApi';
import type { RegisterRequest } from '../types/auth';

vi.mock('@/utils/request', () => ({
	default: {
		post: vi.fn(),
	},
}));

describe('auth API contracts', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('sends the verification email and registration payload to their endpoints', () => {
		const registerPayload: RegisterRequest = {
			username: 'sanjuu',
			email: 'sanjuu@example.com',
			password: 'Password_123',
			verificationCode: '123456',
			privacyPolicyVersion: 'sha256:' + 'a'.repeat(64),
		};

		sendEmailVerificationCode({ email: registerPayload.email });
		register(registerPayload);

		expect(request.post).toHaveBeenNthCalledWith(
			1,
			'auth/email-verification-codes',
			{
				email: registerPayload.email,
			},
			{ timeout: 30000 },
		);
		expect(request.post).toHaveBeenNthCalledWith(2, 'auth/register', registerPayload);
	});

	it('prevents refresh failures from recursively refreshing the access token', () => {
		refreshToken();

		expect(request.post).toHaveBeenCalledWith('auth/refresh', undefined, {
			meta: {
				skipAuthRefresh: true,
				showError: false,
			},
		});
	});
});
