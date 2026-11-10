import type { SeoMetadata } from '../types/seo';

// 同时清理服务端首屏标签与上一页标签，避免文章 OG 信息残留到登录或后台页。
function clearMetadata() {
	document.head.querySelectorAll('[data-seo]').forEach((element) => element.remove());
}

function setMeta(attribute: 'name' | 'property', key: string, value: string | null) {
	if (value === null) return;
	const element = document.createElement('meta');
	element.setAttribute(attribute, key);
	element.content = value;
	element.dataset.seo = 'true';
	document.head.append(element);
}

export function resetPageSeo(title: string) {
	clearMetadata();
	document.title = title;
	setMeta('name', 'robots', 'noindex, follow');
}

export function applyPageSeo(metadata: SeoMetadata) {
	clearMetadata();
	document.title = metadata.title;
	const canonical = document.createElement('link');
	canonical.rel = 'canonical';
	canonical.href = metadata.canonicalUrl;
	canonical.dataset.seo = 'true';
	document.head.append(canonical);
	setMeta('name', 'description', metadata.description);
	setMeta('name', 'robots', 'index, follow');
	setMeta('property', 'og:title', metadata.title);
	setMeta('property', 'og:description', metadata.description);
	setMeta('property', 'og:url', metadata.canonicalUrl);
	setMeta('property', 'og:type', metadata.type);
	setMeta('property', 'og:site_name', import.meta.env.VITE_APP_TITLE || '青禾边');
	setMeta('property', 'og:locale', 'zh_CN');
	setMeta('property', 'og:image', metadata.imageUrl);
	setMeta('property', 'article:published_time', metadata.publishedAt);
	setMeta('property', 'article:modified_time', metadata.updatedAt);
}
