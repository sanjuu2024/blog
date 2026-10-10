import type { Plugin } from 'vite';

// 将开发环境的公开首屏交给后端，仍由 Vite 转换入口脚本并注入 HMR。
export function seoPlugin(backendUrl: string): Plugin {
	return {
		name: 'public-page-seo',
		configureServer(server) {
			server.middlewares.use(async (request, response, next) => {
				const url = request.url || '/';
				const incoming = new URL(url, 'http://localhost');
				const path = incoming.pathname;
				if (
					!['GET', 'HEAD'].includes(request.method || '') ||
					(![
						'/',
						'/articles',
						'/about',
						'/messages',
						'/sitemap.xml',
						'/robots.txt',
					].includes(path) &&
						!/^\/articles\/[^/]+$/.test(path))
				) {
					next();
					return;
				}
				try {
					// 只转发路径与查询，目标主机固定为配置的后端，不能来自请求 URL。
					const target = new URL(backendUrl);
					target.pathname = path;
					target.search = incoming.search;
					const upstream = await fetch(target, {
						method: request.method,
						signal: AbortSignal.timeout(10000),
					});
					const contentType = upstream.headers.get('content-type') || 'text/html';
					const body = await upstream.text();
					response.statusCode = upstream.status;
					response.setHeader('Content-Type', contentType);
					response.setHeader('Cache-Control', 'no-store');
					response.end(
						request.method === 'HEAD'
							? undefined
							: contentType.includes('text/html') && upstream.ok
								? await server.transformIndexHtml(url, body)
								: body,
					);
				} catch {
					response.statusCode = 503;
					response.setHeader('Content-Type', 'text/plain; charset=utf-8');
					response.end('页面暂时不可用，请确认后端已启动');
				}
			});
		},
	};
}
