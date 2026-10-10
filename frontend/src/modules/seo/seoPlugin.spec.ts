// @vitest-environment node
import { createServer as createHttpServer } from 'node:http';
import type { AddressInfo } from 'node:net';
import { createServer as createViteServer } from 'vite';
import { afterEach, describe, expect, it } from 'vitest';
import { seoPlugin } from '../../../plugins/seoPlugin';

describe('seoPlugin', () => {
	let backend: ReturnType<typeof createHttpServer>;
	let vite: Awaited<ReturnType<typeof createViteServer>>;
	afterEach(async () => {
		await vite?.close();
		if (backend?.listening)
			await new Promise<void>((resolve) => backend.close(() => resolve()));
	});

	it('transforms server HTML for Vite and forwards robots, query and 404 status', async () => {
		backend = createHttpServer((request, response) => {
			if (request.url === '/robots.txt') {
				response.setHeader('Content-Type', 'text/plain');
				response.end('User-agent: *\nDisallow: /admin\n');
				return;
			}
			if (request.url === '/articles/missing') response.statusCode = 404;
			response.setHeader('Content-Type', 'text/html; charset=utf-8');
			response.end(
				`<html><head><title>文章标题</title></head><body><div id="app"><p>${request.url}</p></div></body></html>`,
			);
		});
		await new Promise<void>((resolve) => backend.listen(0, '127.0.0.1', resolve));
		const backendPort = (backend.address() as AddressInfo).port;
		vite = await createViteServer({
			configFile: false,
			plugins: [seoPlugin(`http://127.0.0.1:${backendPort}`)],
			server: { host: '127.0.0.1', port: 0 },
			logLevel: 'silent',
		});
		await vite.listen();
		const port = (vite.httpServer!.address() as AddressInfo).port;
		const base = `http://127.0.0.1:${port}`;

		const html = await fetch(`${base}/articles/34?replyId=51`);
		expect(html.status).toBe(200);
		const body = await html.text();
		expect(body).toContain('/@vite/client');
		expect(body).toContain('/articles/34?replyId=51');
		expect(html.headers.get('cache-control')).toBe('no-store');
		const robots = await fetch(`${base}/robots.txt`);
		expect(await robots.text()).toContain('Disallow: /admin');
		const missing = await fetch(`${base}/articles/missing`);
		expect(missing.status).toBe(404);
		expect(await missing.text()).not.toContain('/@vite/client');
	});
});
