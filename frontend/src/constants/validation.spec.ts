import { describe, expect, it } from 'vitest';
import {
	ACCOUNT_FORMAT_PATTERN,
	PASSWORD_FORMAT_PATTERN,
	USERNAME_FORMAT_PATTERN,
} from './validation';

describe('credential validation constants', () => {
	it('accepts usernames at the boundaries and without a first-character restriction', () => {
		expect(USERNAME_FORMAT_PATTERN.test('ab')).toBe(true);
		expect(USERNAME_FORMAT_PATTERN.test('_a')).toBe(true);
		expect(USERNAME_FORMAT_PATTERN.test('-x')).toBe(true);
		expect(USERNAME_FORMAT_PATTERN.test('a'.repeat(20))).toBe(true);
	});

	it('rejects usernames with invalid lengths or unsupported characters', () => {
		expect(USERNAME_FORMAT_PATTERN.test('a')).toBe(false);
		expect(USERNAME_FORMAT_PATTERN.test('a'.repeat(21))).toBe(false);
		expect(USERNAME_FORMAT_PATTERN.test('中文用户名')).toBe(false);
		expect(USERNAME_FORMAT_PATTERN.test('alice user')).toBe(false);
		expect(USERNAME_FORMAT_PATTERN.test('alice.dev')).toBe(false);
	});

	it('accepts either a valid username or a valid email as the login account', () => {
		expect(ACCOUNT_FORMAT_PATTERN.test('ab')).toBe(true);
		expect(ACCOUNT_FORMAT_PATTERN.test('alice@example.com')).toBe(true);
		expect(ACCOUNT_FORMAT_PATTERN.test('中文用户')).toBe(false);
		expect(ACCOUNT_FORMAT_PATTERN.test('alice@')).toBe(false);
	});

	it('accepts supported passwords at both length boundaries', () => {
		expect(PASSWORD_FORMAT_PATTERN.test('Aa123!')).toBe(true);
		expect(PASSWORD_FORMAT_PATTERN.test('A'.repeat(32))).toBe(true);

		const allowedSpecialCharacters = `!@#$%^&*()+=[]{}:;'",.?/~\`|\\<>-_`;
		for (const character of allowedSpecialCharacters) {
			expect(PASSWORD_FORMAT_PATTERN.test(`Aa123${character}`)).toBe(true);
		}
	});

	it('rejects passwords with invalid lengths, Chinese or whitespace', () => {
		expect(PASSWORD_FORMAT_PATTERN.test('Aa12!')).toBe(false);
		expect(PASSWORD_FORMAT_PATTERN.test('A'.repeat(33))).toBe(false);
		expect(PASSWORD_FORMAT_PATTERN.test('密码Aa1!@')).toBe(false);
		expect(PASSWORD_FORMAT_PATTERN.test('Aa123 456')).toBe(false);
		expect(PASSWORD_FORMAT_PATTERN.test('Aa123\t456')).toBe(false);
	});
});
