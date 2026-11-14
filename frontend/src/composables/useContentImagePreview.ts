import { onScopeDispose, watch, type Ref } from 'vue';
import { useEventListener, useScrollLock } from '@vueuse/core';
import { ElMessage } from 'element-plus';
import type PhotoSwipe from 'photoswipe';
import type { PhotoSwipeEventsMap, SlideData } from 'photoswipe';
import 'photoswipe/style.css';

// 只在当前安全正文中收集图片，不替换 img 或改写作者设置的正文尺寸。
export function useContentImagePreview(
	contentRef: Ref<HTMLElement | null>,
	getHtml: () => string | undefined,
) {
	let viewer: PhotoSwipe | undefined;
	let requestVersion = 0;
	const scrollTarget = () =>
		contentRef.value?.closest<HTMLElement>('.el-dialog') ?? document.documentElement;
	const scrollLocked = useScrollLock(scrollTarget);
	let restoreScrollbarGutter = () => {};

	function destroyPreview() {
		requestVersion++;
		const previous = viewer;
		viewer = undefined;
		previous?.destroy();
	}

	async function openPreview(image: HTMLImageElement) {
		const container = contentRef.value;
		if (!container || viewer) return;
		const images = Array.from(container.querySelectorAll<HTMLImageElement>('img[src]'));
		const index = images.indexOf(image);
		if (index < 0) return;
		const version = ++requestVersion;
		try {
			const { default: PhotoSwipe } = await import('photoswipe');
			// 切换正文、离开页面或连续点击后，旧的按需加载不能再打开画廊。
			if (version !== requestVersion || container !== contentRef.value) return;
			const dataSource: SlideData[] = images.map((item) => ({
				src: item.currentSrc || item.src,
				width: item.naturalWidth,
				height: item.naturalHeight,
				alt: item.alt,
				element: item,
			}));
			// 嵌套在后台弹窗内，避免两个焦点陷阱互相把焦点抢回背景。
			const dialog = container.closest<HTMLElement>('.el-dialog');
			const current = new PhotoSwipe({
				dataSource,
				index,
				appendToEl: dialog ?? document.body,
				zoom: false,
				wheelToZoom: true,
				returnFocus: false,
				closeTitle: '关闭',
				arrowPrevTitle: '上一张',
				arrowNextTitle: '下一张',
				errorMsg: '图片加载失败',
			});
			viewer = current;
			const scrollContainer = scrollTarget();
			const originalGutter = scrollContainer.style.scrollbarGutter;
			restoreScrollbarGutter = () => {
				scrollContainer.style.scrollbarGutter = originalGutter;
			};
			// 隐藏滚动条时仍保留原有占位；短页面不额外增加空白。
			if (
				scrollContainer.scrollHeight > scrollContainer.clientHeight &&
				!getComputedStyle(scrollContainer).scrollbarGutter.includes('stable')
			) {
				scrollContainer.style.scrollbarGutter = 'stable';
			}
			scrollLocked.value = true;

			function updateDimensions(content: PhotoSwipeEventsMap['loadComplete']['content']) {
				const loadedImage = content.element;
				if (
					!(loadedImage instanceof HTMLImageElement) ||
					!loadedImage.naturalWidth ||
					!loadedImage.naturalHeight
				)
					return;
				const { naturalWidth: width, naturalHeight: height } = loadedImage;
				if (content.width === width && content.height === height) return;
				Object.assign(content.data, { width, height });
				Object.assign(content, { width, height });
				const slide = content.slide;
				if (!slide) return;
				Object.assign(slide, { width, height, currentResolution: 0 });
				slide.calculateSize();
				slide.zoomAndPanToInitial();
				slide.updateContentSize(true);
				slide.applyCurrentZoomPan();
			}

			// 尚未加载的正文图片由 PhotoSwipe 按需读取；取得像素尺寸后交回库重新布局。
			current.on('contentLoadImage', ({ content }) => {
				if (!content.width && content.element) {
					current.events.add(content.element, 'load', () => updateDimensions(content));
				}
			});
			current.on('loadComplete', ({ content, isError }) => {
				if (!isError) updateDimensions(content);
			});
			current.on('openingAnimationEnd', () => {
				// 库在打开动画期间不能立即 destroy，动画结束后完成已取消任务的清理。
				if (viewer !== current) current.destroy();
			});
			const stopEscape = useEventListener(
				document,
				'keydown',
				(event: KeyboardEvent) => {
					if (event.key !== 'Escape') return;
					// 在捕获阶段隔离 Esc，不能同时触发后台弹窗或目录的关闭监听。
					event.preventDefault();
					event.stopImmediatePropagation();
					current.close();
				},
				{ capture: true },
			);
			current.on('destroy', () => {
				stopEscape();
				if (!viewer || viewer === current) {
					scrollLocked.value = false;
					restoreScrollbarGutter();
					viewer = undefined;
					if (image.isConnected) image.focus({ preventScroll: true });
				}
			});
			current.init();
		} catch {
			if (version !== requestVersion) return;
			destroyPreview();
			scrollLocked.value = false;
			restoreScrollbarGutter();
			ElMessage.error('图片预览加载失败，请重试');
		}
	}

	useEventListener(contentRef, 'click', (event: MouseEvent) => {
		if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey)
			return;
		if (!(event.target instanceof HTMLImageElement) || !event.target.getAttribute('src'))
			return;
		event.preventDefault();
		void openPreview(event.target);
	});
	useEventListener(contentRef, 'keydown', (event: KeyboardEvent) => {
		if (event.key !== 'Enter' && event.key !== ' ') return;
		if (!(event.target instanceof HTMLImageElement)) return;
		event.preventDefault();
		void openPreview(event.target);
	});
	watch(
		[contentRef, getHtml],
		([container]) => {
			destroyPreview();
			container?.querySelectorAll<HTMLImageElement>('img[src]').forEach((image) => {
				image.tabIndex = 0;
				image.setAttribute('role', 'button');
				image.setAttribute('aria-label', image.alt ? `预览图片：${image.alt}` : '预览图片');
			});
		},
		{ flush: 'post', immediate: true },
	);
	onScopeDispose(destroyPreview);
}
