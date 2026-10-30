import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ViteDevServer } from 'vite';
import { seoPlugin } from './seoPlugin';

type RequestHandler = (
	request: { url?: string; method?: string },
	response: {
		statusCode: number;
		setHeader: (name: string, value: string) => unknown;
		end: (body?: string) => unknown;
	},
	next: () => void,
) => Promise<void>;

describe('seoPlugin', () => {
	afterEach(() => vi.unstubAllGlobals());

	it('proxies the public link page to the backend HTML and transforms it for Vite', async () => {
		const html = '<html><body><div id="app"></div></body></html>';
		const fetchMock = vi.fn().mockResolvedValue({
			headers: { get: () => 'text/html; charset=UTF-8' },
			text: async () => html,
			ok: true,
			status: 200,
		});
		vi.stubGlobal('fetch', fetchMock);

		const useMiddleware = vi.fn<(handler: unknown) => void>();
		const transformIndexHtml = vi.fn(
			async (_url: string, body: string) => `transformed:${body}`,
		);
		const server = {
			middlewares: { use: useMiddleware },
			transformIndexHtml,
		} as unknown as ViteDevServer;
		const plugin = seoPlugin('http://localhost:8080');
		const configureServer = plugin.configureServer as unknown as (
			server: ViteDevServer,
		) => void;
		configureServer(server);

		const handler = useMiddleware.mock.calls[0]?.[0] as RequestHandler | undefined;
		if (!handler) throw new Error('SEO middleware was not registered');
		const next = vi.fn();
		const response = {
			statusCode: 200,
			setHeader: vi.fn(),
			end: vi.fn(),
		};

		await handler({ method: 'GET', url: '/links' }, response, next);

		expect(next).not.toHaveBeenCalled();
		expect(fetchMock).toHaveBeenCalledWith(
			new URL('http://localhost:8080/links'),
			expect.objectContaining({ method: 'GET' }),
		);
		expect(transformIndexHtml).toHaveBeenCalledWith('/links', html);
		expect(response.statusCode).toBe(200);
		expect(response.end).toHaveBeenCalledWith(`transformed:${html}`);
	});

	it('proxies the RSS document without transforming it as HTML', async () => {
		const rss = '<?xml version="1.0"?><rss version="2.0"></rss>';
		const fetchMock = vi.fn().mockResolvedValue({
			headers: { get: () => 'application/rss+xml; charset=UTF-8' },
			text: async () => rss,
			ok: true,
			status: 200,
		});
		vi.stubGlobal('fetch', fetchMock);

		const useMiddleware = vi.fn<(handler: unknown) => void>();
		const transformIndexHtml = vi.fn(
			async (_url: string, body: string) => `transformed:${body}`,
		);
		const server = {
			middlewares: { use: useMiddleware },
			transformIndexHtml,
		} as unknown as ViteDevServer;
		const plugin = seoPlugin('http://localhost:8080');
		const configureServer = plugin.configureServer as unknown as (
			server: ViteDevServer,
		) => void;
		configureServer(server);

		const handler = useMiddleware.mock.calls[0]?.[0] as RequestHandler | undefined;
		if (!handler) throw new Error('SEO middleware was not registered');
		const next = vi.fn();
		const response = {
			statusCode: 200,
			setHeader: vi.fn(),
			end: vi.fn(),
		};

		await handler({ method: 'GET', url: '/rss.xml' }, response, next);

		expect(next).not.toHaveBeenCalled();
		expect(fetchMock).toHaveBeenCalledWith(
			new URL('http://localhost:8080/rss.xml'),
			expect.objectContaining({ method: 'GET' }),
		);
		expect(response.setHeader).toHaveBeenCalledWith(
			'Content-Type',
			'application/rss+xml; charset=UTF-8',
		);
		expect(transformIndexHtml).not.toHaveBeenCalled();
		expect(response.end).toHaveBeenCalledWith(rss);
	});
});
