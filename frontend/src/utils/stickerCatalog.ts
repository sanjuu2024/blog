import type { StickerPack } from '@/types/sticker';

export function applyStickerBaseUrl(
	packs: StickerPack[],
	configuredBaseUrl?: string,
): StickerPack[] {
	const baseUrl = configuredBaseUrl?.trim().replace(/\/+$/, '');
	if (!baseUrl) return packs;
	const parsed = new URL(baseUrl);
	if (
		parsed.protocol !== 'https:' ||
		parsed.username ||
		parsed.password ||
		parsed.search ||
		parsed.hash
	) {
		throw new Error('表情 CDN 基址必须是无凭据、查询参数和片段的 HTTPS 地址');
	}
	const resolve = (url: string) => {
		if (!url.startsWith('/stickers/')) throw new Error('表情目录地址必须位于 /stickers/');
		return `${baseUrl}/${url.slice('/stickers/'.length)}`;
	};
	return packs.map((pack) => ({
		...pack,
		iconUrl: resolve(pack.iconUrl),
		items: pack.items.map((item) => ({ ...item, url: resolve(item.url) })),
	}));
}
