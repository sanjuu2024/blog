import { flushPromises, mount, type VueWrapper } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi, type MockInstance } from 'vitest';
import PhotoSwipe from 'photoswipe';
import ArticleBody from '@/modules/article/components/ArticleBody.vue';
import AdminArticlePreviewDialog from '@/modules/admin/article/components/AdminArticlePreviewDialog.vue';
import AboutPage from '@/modules/about/views/AboutPage.vue';
import { getAboutPage } from '@/modules/about/api/aboutApi';

vi.mock('@/utils/prism', () => ({ highlightCodeUnder: vi.fn() }));
vi.mock('@/modules/about/api/aboutApi', () => ({ getAboutPage: vi.fn() }));

function setDimensions(image: HTMLImageElement, width = 1600, height = 1000) {
	Object.defineProperties(image, {
		naturalWidth: { configurable: true, value: width },
		naturalHeight: { configurable: true, value: height },
	});
}

describe('content image preview with PhotoSwipe', () => {
	let wrapper: VueWrapper;
	let init: MockInstance<PhotoSwipe['init']>;

	beforeEach(() => {
		Object.defineProperty(document.documentElement, 'clientWidth', {
			configurable: true,
			value: 1024,
		});
		Object.defineProperties(document.documentElement, {
			clientHeight: { configurable: true, value: 768 },
			scrollHeight: { configurable: true, value: 1600 },
		});
		vi.stubGlobal(
			'matchMedia',
			vi.fn((query: string) => ({
				matches: query.includes('prefers-reduced-motion'),
			})),
		);
		init = vi.spyOn(PhotoSwipe.prototype, 'init');
	});

	afterEach(async () => {
		wrapper?.unmount();
		await vi.waitFor(() => expect(document.querySelector('.pswp')).toBeNull());
		vi.restoreAllMocks();
		vi.unstubAllGlobals();
	});

	it('opens the clicked occurrence using pixel dimensions, preserves links and captions, and uses built-in wheel zoom', async () => {
		const outsideImage = document.createElement('img');
		outsideImage.src = '/cover.png';
		document.body.append(outsideImage);
		wrapper = mount(ArticleBody, {
			attachTo: document.body,
			props: {
				html: '<p><img src="/same.png" width="50%" title="图注"></p><p><a href="https://example.com/"><img src="/same.png" alt="第二张"></a></p>',
			},
		});
		await flushPromises();
		const images = wrapper.findAll<HTMLImageElement>('.article-markdown img');
		images.forEach(({ element }) => setDimensions(element));
		const modifiedClick = new MouseEvent('click', {
			bubbles: true,
			cancelable: true,
			ctrlKey: true,
		});
		let intercepted: boolean | undefined;
		// 记录正文监听是否拦截，再阻止 jsdom 执行不支持的新页面导航。
		document.addEventListener(
			'click',
			(event) => {
				intercepted = event.defaultPrevented;
				event.preventDefault();
			},
			{ once: true },
		);
		images[1].element.dispatchEvent(modifiedClick);
		await flushPromises();
		expect(intercepted).toBe(false);
		expect(init).not.toHaveBeenCalled();

		await images[1].trigger('click');
		await vi.dynamicImportSettled();
		await flushPromises();
		const viewer = init.mock.contexts[0] as PhotoSwipe;
		await vi.waitFor(() => expect(viewer.opener.isOpen).toBe(true));
		expect(viewer.getNumItems()).toBe(2);
		expect(viewer.currIndex).toBe(1);
		expect(viewer.options).toMatchObject({ zoom: false, wheelToZoom: true });
		expect(viewer.getItemData(0)).toMatchObject({ width: 1600, height: 1000 });
		expect(document.querySelector<HTMLElement>('.pswp__counter')?.innerText).toBe('2 / 2');
		expect(document.querySelector('.pswp__button--zoom')).toBeNull();
		expect(images[0].attributes('width')).toBe('50%');
		expect(wrapper.get('.article-image__caption').text()).toBe('图注');
		expect(wrapper.get('a').attributes('href')).toBe('https://example.com/');
		expect(document.documentElement.style.overflow).toBe('hidden');
		expect(document.documentElement.style.scrollbarGutter).toBe('stable');
		const initialZoom = viewer.currSlide!.currZoomLevel;
		viewer.element!.dispatchEvent(
			new WheelEvent('wheel', {
				bubbles: true,
				cancelable: true,
				deltaY: -100,
				clientX: 400,
				clientY: 300,
			}),
		);
		expect(viewer.currSlide!.currZoomLevel).toBeGreaterThan(initialZoom);
		viewer.prev();
		expect(viewer.currIndex).toBe(0);
		viewer.close();
		await vi.waitFor(() => expect(document.querySelector('.pswp')).toBeNull());
		expect(document.documentElement.style.overflow).toBe('');
		expect(document.documentElement.style.scrollbarGutter).toBe('');
		expect(document.activeElement).toBe(images[1].element);
		outsideImage.remove();
	});

	it('opens without waiting for other images, fills unknown dimensions on load and keeps errors navigable', async () => {
		wrapper = mount(ArticleBody, {
			attachTo: document.body,
			props: {
				html: '<img src="/ready.png"><img src="/pending.png" width="25%"><img src="/broken.png">',
			},
		});
		await flushPromises();
		setDimensions(wrapper.get<HTMLImageElement>('img').element);
		await wrapper.get('img').trigger('click');
		await vi.dynamicImportSettled();
		await flushPromises();
		const viewer = init.mock.contexts[0] as PhotoSwipe;
		expect(viewer.currIndex).toBe(0);
		expect(viewer.getNumItems()).toBe(3);
		viewer.next();
		const content = viewer.currSlide!.content;
		setDimensions(content.element as HTMLImageElement, 2400, 1800);
		content.element!.dispatchEvent(new Event('load'));
		expect(viewer.currSlide).toMatchObject({ width: 2400, height: 1800 });
		expect(content.data).toMatchObject({ width: 2400, height: 1800 });
		expect(viewer.currSlide!.zoomLevels.initial).toBeGreaterThan(0);
		expect(wrapper.get('img[width]').attributes('width')).toBe('25%');
		viewer.next();
		viewer.currSlide!.content.element!.dispatchEvent(new Event('error'));
		expect(viewer.currSlide!.content.isError()).toBe(true);
		expect(document.querySelector('.pswp__error-msg')).not.toBeNull();
		viewer.prev();
		expect(viewer.currIndex).toBe(1);
	});

	it('supports keyboard opening, cancels old imports and destroys the gallery on content change and unmount', async () => {
		wrapper = mount(ArticleBody, {
			attachTo: document.body,
			props: { html: '<img src="/old.png">' },
		});
		await flushPromises();
		const image = wrapper.get<HTMLImageElement>('img');
		expect(image.attributes('tabindex')).toBe('0');
		expect(image.attributes('aria-label')).toBe('预览图片');
		await image.trigger('click');
		await wrapper.setProps({ html: '<img src="/new.png">' });
		await vi.dynamicImportSettled();
		await flushPromises();
		expect(document.querySelector('.pswp')).toBeNull();
		const newImage = wrapper.get<HTMLImageElement>('img');
		setDimensions(newImage.element);
		document.documentElement.style.scrollbarGutter = 'stable both-edges';
		await newImage.trigger('keydown', { key: 'Enter' });
		await vi.dynamicImportSettled();
		await flushPromises();
		expect(document.querySelector('.pswp')).not.toBeNull();
		expect(document.documentElement.style.scrollbarGutter).toBe('stable both-edges');
		await wrapper.setProps({ html: '<p>没有图片</p>' });
		await vi.waitFor(() => expect(document.querySelector('.pswp')).toBeNull());
		expect(document.documentElement.style.scrollbarGutter).toBe('stable both-edges');
		document.documentElement.style.scrollbarGutter = '';
		await wrapper.setProps({ html: '<img src="/last.png">' });
		setDimensions(wrapper.get<HTMLImageElement>('img').element);
		await wrapper.get('img').trigger('click');
		await vi.dynamicImportSettled();
		await flushPromises();
		wrapper.unmount();
		await vi.waitFor(() => expect(document.querySelector('.pswp')).toBeNull());
		expect(document.documentElement.style.overflow).toBe('');
	});

	it('keeps focus in the nested gallery and Escape closes only images before the real admin dialog', async () => {
		wrapper = mount(AdminArticlePreviewDialog, {
			attachTo: document.body,
			props: {
				modelValue: true,
				loading: false,
				failed: false,
				article: {
					title: '预览',
					summary: '',
					coverUrl: '/cover.png',
					categoryPath: '',
					tags: [],
					contentHtml: '<h2>章节</h2><img src="/body.png">',
				},
			},
			global: { stubs: { transition: false } },
		});
		await flushPromises();
		const image = wrapper.get<HTMLImageElement>('.article-markdown img');
		setDimensions(image.element);
		const dialog = wrapper.get<HTMLElement>('.el-dialog').element;
		Object.defineProperties(dialog, {
			clientHeight: { configurable: true, value: 768 },
			scrollHeight: { configurable: true, value: 1600 },
		});
		await image.trigger('click');
		await vi.dynamicImportSettled();
		await flushPromises();
		const viewer = init.mock.contexts[0] as PhotoSwipe;
		await vi.waitFor(() => expect(viewer.opener.isOpen).toBe(true));
		expect(viewer.options.appendToEl).toBe(dialog);
		expect(viewer.getNumItems()).toBe(1);
		expect(dialog.style.overflow).toBe('hidden');
		expect(dialog.style.scrollbarGutter).toBe('stable');
		expect(viewer.element!.contains(document.activeElement)).toBe(true);
		const backgroundEscape = vi.fn();
		document.addEventListener('keydown', backgroundEscape);
		try {
			document.dispatchEvent(
				new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape', bubbles: true }),
			);
			await flushPromises();
			await vi.waitFor(() => expect(document.querySelector('.pswp')).toBeNull());
			expect(wrapper.emitted('update:modelValue')).toBeUndefined();
			expect(backgroundEscape).not.toHaveBeenCalled();
			expect(document.activeElement).toBe(image.element);
			expect(dialog.style.overflow).toBe('');
			expect(dialog.style.scrollbarGutter).toBe('');
			document.dispatchEvent(
				new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape', bubbles: true }),
			);
			await flushPromises();
			await vi.waitFor(() =>
				expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual([false]),
			);
		} finally {
			document.removeEventListener('keydown', backgroundEscape);
		}
	});

	it('opens only the about body images through the same gallery', async () => {
		vi.mocked(getAboutPage).mockResolvedValueOnce({
			exists: true,
			contentHtml: '<img src="/about.png" width="70%">',
			contentText: '',
			updatedAt: null,
		});
		wrapper = mount(AboutPage, { attachTo: document.body });
		await flushPromises();
		const image = wrapper.get<HTMLImageElement>('.about-content img');
		setDimensions(image.element, 1920, 1080);
		Object.defineProperty(document.documentElement, 'scrollHeight', {
			configurable: true,
			value: 768,
		});
		await image.trigger('click');
		await vi.dynamicImportSettled();
		await flushPromises();
		const viewer = init.mock.contexts[0] as PhotoSwipe;
		expect(viewer.getNumItems()).toBe(1);
		expect(viewer.getItemData(0)).toMatchObject({ width: 1920, height: 1080 });
		expect(image.attributes('width')).toBe('70%');
		expect(document.documentElement.style.scrollbarGutter).toBe('');
	});
});
