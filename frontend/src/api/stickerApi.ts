import request from '@/utils/request';
import type { ApiResult } from '@/types/api';
import type { StickerPack } from '@/types/sticker';
import { applyStickerBaseUrl } from '@/utils/stickerCatalog';

export function getStickers(): Promise<StickerPack[]> {
	return request
		.get<ApiResult<StickerPack[]>, StickerPack[]>('comments/stickers', {
			meta: { showError: false },
		})
		.then((packs) => applyStickerBaseUrl(packs, import.meta.env.VITE_STICKER_BASE_URL));
}
