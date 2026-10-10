import { watch, onScopeDispose } from 'vue';
import { useRoute } from 'vue-router';
import { getSeoMetadata } from '../api/seoApi';
import { applyPageSeo, resetPageSeo } from '../utils/pageSeo';

export function usePageSeo() {
	const route = useRoute();
	let requestVersion = 0;
	let firstNavigation = true;

	watch(
		() => (route.matched.length ? route.path : null),
		async (path) => {
			if (path === null) return;
			const version = ++requestVersion;
			const siteName = import.meta.env.VITE_APP_TITLE || '青禾边';
			// 初始导航完成后保留已匹配当前页面的服务端标签，避免先清空再重复请求。
			const canonical = document.head.querySelector<HTMLLinkElement>(
				'link[data-seo][rel=canonical]',
			);
			const preserveInitial =
				firstNavigation && canonical && new URL(canonical.href).pathname === path;
			firstNavigation = false;
			if (!preserveInitial) {
				resetPageSeo(route.meta.title ? `${route.meta.title} - ${siteName}` : siteName);
			}
			// 文章页面从详情响应同步 SEO；query/hash 不改变规范地址，也不触发此监听。
			if (preserveInitial || !['/', '/articles', '/about', '/messages'].includes(path))
				return;
			try {
				const metadata = await getSeoMetadata(path);
				// 快速跨文章导航时仅应用当前请求结果，query/hash 不改变规范地址。
				if (version !== requestVersion) return;
				applyPageSeo(metadata);
			} catch {
				// SEO 请求失败不阻塞正文和交互；默认 noindex，避免收录不存在的页面。
			}
		},
		{ immediate: true, flush: 'sync' },
	);

	onScopeDispose(() => {
		requestVersion++;
	});
}
