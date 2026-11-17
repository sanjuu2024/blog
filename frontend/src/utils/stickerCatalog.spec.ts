import { describe, expect, it } from 'vitest';
import { applyStickerBaseUrl } from './stickerCatalog';
import type { StickerPack } from '@/types/sticker';

const packs: StickerPack[] = [
	{
		id: 'test',
		name: '测试',
		minHeightRem: 3,
		iconUrl: '/stickers/test/t_%E7%94%B2.png?v=0123456789abcdef',
		items: [
			{
				token: '[sticker:test:甲]',
				name: '甲',
				url: '/stickers/test/t_%E7%94%B2.png?v=0123456789abcdef',
			},
		],
	},
];

describe('sticker CDN configuration', () => {
	it('keeps same-origin URLs when no CDN base is configured', () => {
		expect(applyStickerBaseUrl(packs, '')).toBe(packs);
	});
	it('rewrites icons and images without losing encoding, content versions or tokens', () => {
		const result = applyStickerBaseUrl(packs, ' https://sticker.ccsanjuu.com/ ');
		expect(result[0]?.iconUrl).toBe(
			'https://sticker.ccsanjuu.com/test/t_%E7%94%B2.png?v=0123456789abcdef',
		);
		expect(result[0]?.items[0]).toEqual({ ...packs[0]?.items[0], url: result[0]?.iconUrl });
		expect(packs[0]?.iconUrl).toMatch(/^\/stickers\//);
	});
	it.each([
		'http://example.com',
		'https://user:password@example.com',
		'https://example.com?q=1',
		'https://example.com/#x',
	])('rejects invalid CDN configuration: %s', (base) => {
		expect(() => applyStickerBaseUrl(packs, base)).toThrow();
	});
});
