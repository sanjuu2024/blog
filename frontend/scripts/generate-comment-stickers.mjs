import { readdir, readFile, writeFile, mkdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import process from 'node:process';

const frontendRoot = fileURLToPath(new URL('../', import.meta.url));
const sourceRoot = path.join(frontendRoot, 'public/stickers');
const target = path.resolve(
	frontendRoot,
	'../backend/src/main/resources/comment-stickers/catalog.json',
);
const packs = [];

for (const directory of (await readdir(sourceRoot, { withFileTypes: true })).sort((a, b) =>
	a.name.localeCompare(b.name, 'en'),
)) {
	if (!directory.isDirectory()) continue;
	const id = directory.name;
	if (!/^[a-z0-9_-]+$/.test(id)) throw new Error(`表情库目录名不合法：${id}`);
	const root = path.join(sourceRoot, id);
	const info = JSON.parse(await readFile(path.join(root, 'info.json'), 'utf8'));
	const files = new Set(await readdir(root));
	if (!['png', 'gif', 'webp'].includes(info.type)) throw new Error(`表情格式不支持：${id}`);
	if (!info.name || !Array.isArray(info.items) || !info.items.length)
		throw new Error(`表情清单为空：${id}`);
	if (new Set(info.items).size !== info.items.length) throw new Error(`表情清单重复：${id}`);
	if (
		info.gridMinWidthRem !== undefined &&
		(typeof info.gridMinWidthRem !== 'number' ||
			!Number.isFinite(info.gridMinWidthRem) ||
			info.gridMinWidthRem <= 0)
	)
		throw new Error(`表情库 gridMinWidthRem 必须为正数：${id}`);
	function url(item) {
		const filename = `${info.prefix}${item}.${info.type}`;
		if (!files.has(filename)) throw new Error(`表情文件缺失：${id}/${filename}`);
		return `/stickers/${id}/${encodeURIComponent(filename)}`;
	}
	packs.push({
		id,
		name: info.name,
		iconUrl: url(info.icon),
		...(info.gridMinWidthRem === undefined ? {} : { gridMinWidthRem: info.gridMinWidthRem }),
		items: info.items.map((name) => {
			const token = `[sticker:${id}:${name}]`;
			if (typeof name !== 'string' || /[[\]:\r\n]/.test(name) || token.length > 64)
				throw new Error(`表情标识不合法：${token}`);
			return { token, name, url: url(name) };
		}),
	});
}

const result = JSON.stringify(packs, null, 2) + '\n';
if (process.argv.includes('--check')) {
	if ((await readFile(target, 'utf8')) !== result)
		throw new Error('表情目录与 public/stickers 不一致，请运行 pnpm stickers:generate');
} else {
	await mkdir(path.dirname(target), { recursive: true });
	await writeFile(target, result);
}
console.log(
	`表情目录：${packs.length} 个库，${packs.reduce((sum, pack) => sum + pack.items.length, 0)} 个表情`,
);
