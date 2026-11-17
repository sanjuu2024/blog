import { mkdtemp, mkdir, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { buildStickerCatalog } from './generate-comment-stickers.mjs';

test('content versions are stable and change only with the corresponding image', async () => {
	const root = await mkdtemp(path.join(tmpdir(), 'blog-sticker-catalog-'));
	try {
		await mkdir(path.join(root, 'test'));
		await writeFile(
			path.join(root, 'test/info.json'),
			JSON.stringify({
				name: '测试库',
				type: 'png',
				prefix: 't_',
				icon: '甲',
				items: ['甲', '乙'],
				minHeightRem: 3,
			}),
		);
		await writeFile(path.join(root, 'test/t_甲.png'), 'first image');
		await writeFile(path.join(root, 'test/t_乙.png'), 'second image');
		const first = await buildStickerCatalog(root);
		assert.deepEqual(await buildStickerCatalog(root), first);
		assert.equal(first[0].iconUrl, first[0].items[0].url);
		assert.match(first[0].iconUrl, /^\/stickers\/test\/t_%E7%94%B2\.png\?v=[0-9a-f]{16}$/);
		await writeFile(path.join(root, 'test/t_甲.png'), 'updated image');
		const updated = await buildStickerCatalog(root);
		assert.notEqual(updated[0].items[0].url, first[0].items[0].url);
		assert.equal(updated[0].items[1].url, first[0].items[1].url);
		assert.equal(updated[0].items[0].token, '[sticker:test:甲]');
		assert.equal(updated[0].minHeightRem, 3);
	} finally {
		await rm(root, { recursive: true, force: true });
	}
});
