import { describe, expect, it } from 'vitest';
import {
	countStickerContentLength,
	countStickers,
	insertStickerToken,
	splitStickerContent,
	getStickerContentError,
} from './sticker';

const token = '[sticker:bilibili:doge]';
const sticker = { token, name: 'doge', url: '/stickers/bilibili/bb_doge.png' };
const stickers = new Map([[token, sticker]]);

describe('comment sticker text', () => {
	it('validates messages with the same count rules and business label', () => {
		expect(
			getStickerContentError('字'.repeat(980) + token.repeat(20), stickers, '留言'),
		).toBeNull();
		expect(getStickerContentError(token.repeat(21), stickers, '回复')).toBe(
			'每条回复最多添加 20 个表情',
		);
		expect(getStickerContentError('字'.repeat(1000) + token, stickers, '公告')).toBe(
			'公告内容不能超过 1000 个字符',
		);
		expect(getStickerContentError(' '.repeat(2261), stickers, '留言')).toBe('留言内容不能为空');
	});
	it('counts repeated known stickers and ignores unknown or incomplete tokens', () => {
		expect(countStickers(token.repeat(20), stickers)).toBe(20);
		expect(countStickers(token.repeat(21), stickers)).toBe(21);
		expect(
			countStickers('[sticker:missing:doge]'.repeat(21) + '[sticker:bilibili:doge', stickers),
		).toBe(0);
	});
	it('counts known tokens as one and preserves ordinary UTF-16 counting', () => {
		expect(countStickerContentLength(token.repeat(1000), stickers)).toBe(1000);
		expect(countStickerContentLength('字'.repeat(999) + token, stickers)).toBe(1000);
		expect(countStickerContentLength('😀', stickers)).toBe(2);
		const unknown = '[sticker:missing:doge]';
		expect(countStickerContentLength(unknown, stickers)).toBe(unknown.length);
		expect(countStickerContentLength(token, new Map())).toBe(token.length);
	});

	it('keeps HTML and unknown tokens in safe text parts', () => {
		expect(
			splitStickerContent(`<img onerror=alert(1)>${token}[sticker:missing:doge]`, stickers),
		).toEqual([
			{ text: '<img onerror=alert(1)>' },
			{ text: token, sticker },
			{ text: '[sticker:missing:doge]' },
		]);
	});

	it('replaces the selected text and returns the new caret', () => {
		expect(insertStickerToken('前选中文后', token, 1, 4)).toEqual({
			content: `前${token}后`,
			caret: 1 + token.length,
		});
	});

	it('does not nest or cut existing tokens when inserting', () => {
		expect(insertStickerToken(token, token, 3, 3).content).toBe(token + token);
		expect(insertStickerToken(token + '后', token, 3, token.length + 1).content).toBe(token);
	});
});
