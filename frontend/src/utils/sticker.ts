import type { Sticker } from '@/types/sticker';

export const STICKER_CONTENT_MAX_LENGTH = 1000;
export const STICKER_MAX_COUNT = 20;
export const STICKER_CONTENT_RAW_MAX_LENGTH = 2260;
const TOKEN_PATTERN = /\[sticker:[a-z0-9_-]+:[^[\]:\r\n]+\]/g;

export function getStickerContentError(
	content: string,
	stickers: ReadonlyMap<string, Sticker>,
	label = '评论',
) {
	if (!content.trim()) return `${label}内容不能为空`;
	if (content.length > STICKER_CONTENT_RAW_MAX_LENGTH)
		return `${label}原始内容长度不能超过 2260 个字符`;
	if (countStickers(content, stickers) > STICKER_MAX_COUNT)
		return `每条${label}最多添加 20 个表情`;
	if (countStickerContentLength(content.trim(), stickers) > STICKER_CONTENT_MAX_LENGTH)
		return `${label}内容不能超过 1000 个字符`;
	return null;
}

export type StickerContentPart =
	| { text: string; sticker?: undefined }
	| { text: string; sticker: Sticker };

export function splitStickerContent(content: string, stickers: ReadonlyMap<string, Sticker>) {
	const parts: StickerContentPart[] = [];
	let end = 0;
	for (const match of content.matchAll(TOKEN_PATTERN)) {
		const sticker = stickers.get(match[0]);
		if (!sticker) continue;
		if (match.index > end) parts.push({ text: content.slice(end, match.index) });
		parts.push({ text: match[0], sticker });
		end = match.index + match[0].length;
	}
	if (end < content.length) parts.push({ text: content.slice(end) });
	return parts;
}

export function countStickerContentLength(content: string, stickers: ReadonlyMap<string, Sticker>) {
	return splitStickerContent(content, stickers).reduce(
		(length, part) => length + (part.sticker ? 1 : part.text.length),
		0,
	);
}

export function countStickers(content: string, stickers: ReadonlyMap<string, Sticker>) {
	return splitStickerContent(content, stickers).filter((part) => part.sticker).length;
}

export function insertStickerToken(content: string, token: string, start: number, end: number) {
	// 点击表情时恢复原选区；选区位于标记内部时扩展到边界，避免产生嵌套或残缺标记。
	for (const match of content.matchAll(TOKEN_PATTERN)) {
		const tokenEnd = match.index + match[0].length;
		if (start === end && start > match.index && start < tokenEnd) {
			start = end = tokenEnd;
			break;
		}
		if (start > match.index && start < tokenEnd) start = match.index;
		if (end > match.index && end < tokenEnd) end = tokenEnd;
	}
	return {
		content: content.slice(0, start) + token + content.slice(end),
		caret: start + token.length,
	};
}
