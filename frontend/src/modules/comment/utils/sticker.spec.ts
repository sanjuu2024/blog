import { describe, expect, it } from 'vitest';
import {
	countCommentLength,
	countCommentStickers,
	insertCommentSticker,
	splitCommentContent,
} from './sticker';

const token = '[sticker:bilibili:doge]';
const sticker = { token, name: 'doge', url: '/stickers/bilibili/bb_doge.png' };
const stickers = new Map([[token, sticker]]);

describe('comment sticker text', () => {
	it('counts repeated known stickers and ignores unknown or incomplete tokens', () => {
		expect(countCommentStickers(token.repeat(20), stickers)).toBe(20);
		expect(countCommentStickers(token.repeat(21), stickers)).toBe(21);
		expect(
			countCommentStickers(
				'[sticker:missing:doge]'.repeat(21) + '[sticker:bilibili:doge',
				stickers,
			),
		).toBe(0);
	});
	it('counts known tokens as one and preserves ordinary UTF-16 counting', () => {
		expect(countCommentLength(token.repeat(1000), stickers)).toBe(1000);
		expect(countCommentLength('字'.repeat(999) + token, stickers)).toBe(1000);
		expect(countCommentLength('😀', stickers)).toBe(2);
		const unknown = '[sticker:missing:doge]';
		expect(countCommentLength(unknown, stickers)).toBe(unknown.length);
		expect(countCommentLength(token, new Map())).toBe(token.length);
	});

	it('keeps HTML and unknown tokens in safe text parts', () => {
		expect(
			splitCommentContent(`<img onerror=alert(1)>${token}[sticker:missing:doge]`, stickers),
		).toEqual([
			{ text: '<img onerror=alert(1)>' },
			{ text: token, sticker },
			{ text: '[sticker:missing:doge]' },
		]);
	});

	it('replaces the selected text and returns the new caret', () => {
		expect(insertCommentSticker('前选中文后', token, 1, 4)).toEqual({
			content: `前${token}后`,
			caret: 1 + token.length,
		});
	});

	it('does not nest or cut existing tokens when inserting', () => {
		expect(insertCommentSticker(token, token, 3, 3).content).toBe(token + token);
		expect(insertCommentSticker(token + '后', token, 3, token.length + 1).content).toBe(token);
	});
});
