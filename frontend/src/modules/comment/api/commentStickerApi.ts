import request from '@/utils/request';
import type { ApiResult } from '@/types/api';
import type { CommentStickerPack } from '../types/sticker';

export function getCommentStickers(): Promise<CommentStickerPack[]> {
	return request.get<ApiResult<CommentStickerPack[]>, CommentStickerPack[]>('comments/stickers', {
		meta: { showError: false },
	});
}
