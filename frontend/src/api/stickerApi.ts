import request from '@/utils/request';
import type { ApiResult } from '@/types/api';
import type { StickerPack } from '@/types/sticker';

export function getStickers(): Promise<StickerPack[]> {
	return request.get<ApiResult<StickerPack[]>, StickerPack[]>('comments/stickers', {
		meta: { showError: false },
	});
}
