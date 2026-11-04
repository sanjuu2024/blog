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
			const url = new URL(request.url!, 'http://localhost');
			if (url.pathname === '/home') {
				response.writeHead(308, {
					Location: `/${url.search}`,
					'Cache-Control': 'no-store',
				});
				response.end();
				return;
			}
			if (request.url === '/robots.txt') {
				response.setHeader('Content-Type', 'text/plain');
				response.end('User-agent: *\nDisallow: /admin\n');
				return;
			}
			if (url.pathname === '/articles/missing') {
				response.writeHead(404, { 'Content-Type': 'text/html; charset=utf-8' });
				response.end(
					'<html><head><title>文章不存在</title><meta name="robots" content="noindex, follow" data-seo="true"></head><body><div id="app"></div><script type="module" src="/src/main.ts"></script></body></html>',
				);
				return;
			}
			response.setHeader('Content-Type', 'text/html; charset=utf-8');
			response.end(
				`<html><head><title>文章标题</title><link rel="canonical" href="https://blog.example.com${url.pathname}"></head><body><div id="app"><p>${request.url}</p></div></body></html>`,
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
		const missing = await fetch(`${base}/articles/missing?replyId=51`);
		expect(missing.status).toBe(404);
		expect(missing.headers.get('cache-control')).toBe('no-store');
		const missingBody = await missing.text();
		expect(missingBody).toContain('/@vite/client');
		expect(missingBody).toContain('src="/src/main.ts"');
		expect(missingBody).toContain('<div id="app"></div>');
		expect(missingBody).toContain('content="noindex, follow"');
		expect(missingBody).not.toContain('rel="canonical"');
		const missingHead = await fetch(`${base}/articles/missing`, { method: 'HEAD' });
		expect(missingHead.status).toBe(404);
		expect(await missingHead.text()).toBe('');

		const legacyPath = '/home?from=mail&name=%E9%9D%92%E7%A6%BE';
		for (const method of ['GET', 'HEAD']) {
			const redirect = await fetch(`${base}${legacyPath}`, { method, redirect: 'manual' });
			expect(redirect.status).toBe(308);
			expect(redirect.headers.get('location')).toBe(legacyPath.replace('/home', '/'));
			expect(redirect.headers.get('cache-control')).toBe('no-store');
			expect(await redirect.text()).toBe('');
		}
		const home = await fetch(`${base}${legacyPath}`);
		expect(home.status).toBe(200);
		expect(home.url).toBe(`${base}${legacyPath.replace('/home', '/')}`);
		const homeBody = await home.text();
		expect(homeBody).toContain('/@vite/client');
		expect(homeBody).toContain('rel="canonical" href="https://blog.example.com/"');
	});
});
