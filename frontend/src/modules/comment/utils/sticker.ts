import type { CommentSticker } from '../types/sticker';

export const COMMENT_MAX_LENGTH = 1000;
export const COMMENT_MAX_STICKERS = 20;
export const COMMENT_RAW_MAX_LENGTH = 2260;
const TOKEN_PATTERN = /\[sticker:[a-z0-9_-]+:[^[\]:\r\n]+\]/g;

export type CommentContentPart =
	| { text: string; sticker?: undefined }
	| { text: string; sticker: CommentSticker };

export function splitCommentContent(
	content: string,
	stickers: ReadonlyMap<string, CommentSticker>,
) {
	const parts: CommentContentPart[] = [];
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

export function countCommentLength(content: string, stickers: ReadonlyMap<string, CommentSticker>) {
	return splitCommentContent(content, stickers).reduce(
		(length, part) => length + (part.sticker ? 1 : part.text.length),
		0,
	);
}

export function countCommentStickers(
	content: string,
	stickers: ReadonlyMap<string, CommentSticker>,
) {
	return splitCommentContent(content, stickers).filter((part) => part.sticker).length;
}

export function insertCommentSticker(content: string, token: string, start: number, end: number) {
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
