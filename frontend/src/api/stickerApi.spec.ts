import { afterEach, describe, expect, it, vi } from 'vitest';
import { getStickers } from './stickerApi';
import type { StickerPack } from '@/types/sticker';

const get = vi.hoisted(() => vi.fn());
vi.mock('@/utils/request', () => ({ default: { get } }));

const packs: StickerPack[] = [
	{
		id: 'test',
		name: '测试',
		iconUrl: '/stickers/test/icon.png?v=0123456789abcdef',
		items: [
			{
				token: '[sticker:test:甲]',
				name: '甲',
				url: '/stickers/test/%E7%94%B2.png?v=abcdef0123456789',
			},
		],
	},
];

afterEach(() => {
	vi.unstubAllEnvs();
	get.mockReset();
});

describe('getStickers', () => {
	it.each(['', 'https://sticker.ccsanjuu.com'])(
		'uses the configured image base %s',
		async (base) => {
			vi.stubEnv('VITE_STICKER_BASE_URL', base);
			get.mockResolvedValue(packs);
			const result = await getStickers();
			expect(get).toHaveBeenCalledWith('comments/stickers', { meta: { showError: false } });
			expect(result[0]?.iconUrl).toBe(
				base ? `${base}/test/icon.png?v=0123456789abcdef` : packs[0]?.iconUrl,
			);
			expect(result[0]?.items[0]?.url).toBe(
				base ? `${base}/test/%E7%94%B2.png?v=abcdef0123456789` : packs[0]?.items[0]?.url,
			);
			expect(result[0]?.items[0]?.token).toBe('[sticker:test:甲]');
		},
	);
});
