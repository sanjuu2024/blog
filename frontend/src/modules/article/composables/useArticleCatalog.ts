import type { Ref } from 'vue';
import { onBeforeUnmount, ref } from 'vue';

// 文章目录中的一项，对应 Markdown 正文里的一个标题
export interface ArticleCatalogItem {
	// 标题元素的 id点击目录项时，会用这个 id 找到正文里的标题
	id: string;
	// 标题文本
	text: string;
	// 标题层级（h1: 1, h2: 2...）
	level: number;
}

// 只把 Markdown 标题放进目录
const HEADING_SELECTOR = 'h1, h2, h3, h4, h5, h6';

// 当前章节判定线距离滚动容器顶部的偏移量。
// 这里不只避开 Header，也包含页面顶部留白和标题跳转缓冲。
const ACTIVE_HEADING_OFFSET = 96;

// 和 VitePress 类似，用“滚动位置 + 顶部偏移”作为阅读线
// 已经越过这条线的最后一个标题，就是当前章节
const ACTIVE_HEADING_EXTRA_OFFSET = 4;

// 页面底部误差。最后一个标题后面内容很短时，它可能无法滚到阅读线，
// 所以只有真正接近滚动容器底部时，才额外高亮最后一个标题
const SCROLL_BOTTOM_THRESHOLD = 2;

// 页面滚动容器可能不是 window。
// 例如当前前台布局里，真正滚动的是 .app-layout，而不是 window。
type ScrollContainer = Window | HTMLElement;

// 帮 TypeScript 明确区分 window 和普通 DOM 元素。
// 只有 HTMLElement 才有 scrollTop / clientHeight / scrollHeight 等属性。
function isElementScrollContainer(container: ScrollContainer): container is HTMLElement {
	return container instanceof HTMLElement;
}

// 根据标题文字生成一个可用作 DOM id 的字符串
// 例如“Vue 响应式原理！”会变成“vue-响应式原理”
function normalizeHeadingId(text: string, index: number) {
	return (
		text
			.trim()
			.toLowerCase()
			.replace(/\s+/g, '-')
			.replace(/[^\p{L}\p{N}-]/gu, '') || `heading-${index + 1}`
	);
}

// 保证标题 id 不重复
// 如果文章中出现两个同名标题，第二个会自动追加 -2，第三个追加 -3
function createUniqueHeadingId(baseId: string, usedIds: Set<string>) {
	let id = baseId;
	let count = 2;

	while (usedIds.has(id)) {
		id = `${baseId}-${count}`;
		count += 1;
	}

	usedIds.add(id);
	return id;
}

// 从已经渲染到页面上的文章 HTML 中扫描标题，生成目录数据
// 这里写成 composable，而不是 utils，是因为它需要维护响应式 catalogList，
// 也依赖组件传入的 DOM ref
export function useArticleCatalog(contentRef: Ref<HTMLElement | null>) {
	const catalogList = ref<ArticleCatalogItem[]>([]);
	const activeCatalogId = ref('');

	// 保存当前文章正文里的标题 DOM
	// 这样滚动兜底逻辑不用每次都重新 querySelectorAll
	let headingElements: HTMLHeadingElement[] = [];
	let scrollContainer: ScrollContainer | null = null;
	let scrollRafId = 0;

	// 清理上一篇文章或组件销毁前注册过的滚动监听
	// 如果不清理，切换文章后旧标题还可能继续影响 activeCatalogId
	function stopObserveActiveHeading() {
		scrollContainer?.removeEventListener('scroll', updateActiveHeadingByScroll);
		window.removeEventListener('resize', updateActiveHeadingByScroll);
		scrollContainer = null;

		if (scrollRafId) {
			window.cancelAnimationFrame(scrollRafId);
			scrollRafId = 0;
		}
	}

	// 判断一个元素是不是页面设计上的纵向滚动容器。
	// 这里只看 overflow-y，而不再要求 scrollHeight > clientHeight。
	// 原因是文章图片、代码块等内容可能后加载：初次扫描目录时内容还没撑开，
	// 如果此时要求“已经能滚动”，就可能错过真正的滚动容器 .app-layout。
	function isScrollableElement(element: HTMLElement) {
		const { overflowY } = window.getComputedStyle(element);

		return overflowY === 'auto' || overflowY === 'scroll';
	}

	// 从文章正文向上找最近的真实滚动容器。
	// 找不到时再退回 window，兼容以后页面改回 window 滚动的情况。
	function getScrollContainer() {
		let element = contentRef.value?.parentElement ?? null;

		while (element) {
			if (isScrollableElement(element)) return element;

			element = element.parentElement;
		}

		return window;
	}

	// 当前滚动容器已经滚过的距离。
	// window 用 scrollY；普通元素用自己的 scrollTop。
	function getContainerScrollTop(container: ScrollContainer) {
		return isElementScrollContainer(container) ? container.scrollTop : window.scrollY;
	}

	// 当前滚动容器可视区域高度。
	// window 用 innerHeight；普通元素用 clientHeight。
	function getContainerClientHeight(container: ScrollContainer) {
		return isElementScrollContainer(container) ? container.clientHeight : window.innerHeight;
	}

	// 当前滚动容器内容总高度。
	// window 需要兼容 body / documentElement 谁撑高页面；普通元素用 scrollHeight。
	function getContainerScrollHeight(container: ScrollContainer) {
		if (isElementScrollContainer(container)) return container.scrollHeight;

		const body = document.body;
		const documentElement = document.documentElement;

		return Math.max(
			body.scrollHeight,
			body.offsetHeight,
			documentElement.clientHeight,
			documentElement.scrollHeight,
			documentElement.offsetHeight,
		);
	}

	// 标题相对滚动容器内容顶部的绝对位置。
	// 普通元素滚动时，headingRect.top 和 containerRect.top 都是相对视口的，
	// 二者相减再加 scrollTop，就能得到标题在容器内部的真实 top。
	function getHeadingTopInContainer(heading: HTMLHeadingElement, container: ScrollContainer) {
		const headingTop = heading.getBoundingClientRect().top;

		if (!isElementScrollContainer(container)) return headingTop + window.scrollY;

		const containerTop = container.getBoundingClientRect().top;
		return headingTop - containerTop + container.scrollTop;
	}

	// 滚动时根据标题绝对位置算当前章节。
	// 这个思路和 VitePress 类似：不是判断“哪个标题可见”，而是判断
	// “当前滚动位置已经经过了哪个标题，且还没到下一个标题”。
	function syncActiveHeading() {
		scrollRafId = 0;

		if (!headingElements.length) {
			activeCatalogId.value = '';
			return;
		}

		const container = scrollContainer ?? getScrollContainer();
		const scrollTop = getContainerScrollTop(container);
		const clientHeight = getContainerClientHeight(container);
		const scrollHeight = getContainerScrollHeight(container);
		const isContainerBottom =
			scrollTop > 0 && scrollTop + clientHeight >= scrollHeight - SCROLL_BOTTOM_THRESHOLD;

		if (isContainerBottom) {
			activeCatalogId.value = headingElements[headingElements.length - 1].id;
			return;
		}

		const readingLine = scrollTop + ACTIVE_HEADING_OFFSET + ACTIVE_HEADING_EXTRA_OFFSET;
		let activeId = '';

		for (const heading of headingElements) {
			if (getHeadingTopInContainer(heading, container) > readingLine) break;

			activeId = heading.id;
		}

		activeCatalogId.value = activeId;
	}

	// scroll 事件可能触发得很频繁，所以这里不直接计算
	// requestAnimationFrame 会把同一帧里的多次滚动合并成一次计算
	function updateActiveHeadingByScroll() {
		if (scrollRafId) return;

		scrollRafId = window.requestAnimationFrame(syncActiveHeading);
	}

	// 开始同步当前目录项。
	// 这里不用 IntersectionObserver 直接决定高亮项，因为文章标题密集时，
	// “可见”不等于“当前章节”，容易出现跳过标题的问题。
	function startObserveActiveHeading() {
		stopObserveActiveHeading();

		if (!headingElements.length) {
			activeCatalogId.value = '';
			return;
		}

		scrollContainer = getScrollContainer();
		scrollContainer.addEventListener('scroll', updateActiveHeadingByScroll, {
			passive: true,
		});
		window.addEventListener('resize', updateActiveHeadingByScroll, { passive: true });
		syncActiveHeading();
	}

	// 在 article.contentHtml 更新，并且 v-html 完成渲染后调用
	// 它会：
	// 1. 找到正文里的 h1~h6；
	// 2. 给每个标题补上唯一 id；
	// 3. 把标题转换成侧栏可以渲染的目录数组
	function refreshCatalog() {
		const contentEl = contentRef.value;

		if (!contentEl) {
			catalogList.value = [];
			headingElements = [];
			stopObserveActiveHeading();
			activeCatalogId.value = '';
			return;
		}

		const usedIds = new Set<string>();
		const nextHeadingElements: HTMLHeadingElement[] = [];

		catalogList.value = Array.from(
			contentEl.querySelectorAll<HTMLHeadingElement>(HEADING_SELECTOR),
		)
			.map((heading, index) => {
				const text = heading.textContent?.trim() ?? '';

				// 空标题没有可读价值，不放进目录
				if (!text) return null;

				// 如果后端生成的 HTML 已经有 id，就优先沿用；
				// 否则根据标题文本生成一个 id
				const baseId = heading.id || normalizeHeadingId(text, index);
				const id = createUniqueHeadingId(baseId, usedIds);

				heading.id = id;
				nextHeadingElements.push(heading);

				return {
					id,
					text,
					level: Number(heading.tagName.slice(1)),
				};
			})
			.filter((item): item is ArticleCatalogItem => Boolean(item));

		headingElements = nextHeadingElements;
		startObserveActiveHeading();
	}

	// 点击目录项时调用，滚动到正文中对应的标题
	function scrollToHeading(id: string) {
		const heading = document.getElementById(id);
		const container = scrollContainer ?? getScrollContainer();

		if (!heading) return;

		// window 滚动可以继续使用浏览器原生 scrollIntoView，
		// CSS 里的 scroll-margin-top 会自动处理固定 Header 遮挡。
		if (container === window) {
			heading.scrollIntoView({
				behavior: 'smooth',
				block: 'start',
			});
			return;
		}

		// 元素容器滚动时，scrollIntoView 可能会顺带影响外层页面。
		// 这里直接滚动真实容器，位置更可控。
		container.scrollTo({
			top:
				getHeadingTopInContainer(heading as HTMLHeadingElement, container) -
				ACTIVE_HEADING_OFFSET,
			behavior: 'smooth',
		});
	}

	onBeforeUnmount(() => {
		stopObserveActiveHeading();
	});

	return {
		activeCatalogId,
		catalogList,
		refreshCatalog,
		scrollToHeading,
	};
}
