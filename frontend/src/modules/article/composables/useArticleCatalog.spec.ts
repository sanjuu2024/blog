import { mount } from '@vue/test-utils';
import { defineComponent, ref } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ACTIVE_HEADING_OFFSET, useArticleCatalog } from './useArticleCatalog';

const CatalogHarness = defineComponent({
	setup() {
		const contentRef = ref<HTMLElement | null>(null);
		const catalog = useArticleCatalog(contentRef);
		return { contentRef, ...catalog };
	},
	template: `
		<div class="scroll-container">
			<div ref="contentRef">
				<h2>重复 标题！</h2>
				<h3>重复 标题！</h3>
				<h4 id="custom-id">自定义标题</h4>
				<h5>   </h5>
			</div>
		</div>
	`,
});

const WindowCatalogHarness = defineComponent({
	setup() {
		const contentRef = ref<HTMLElement | null>(null);
		const catalog = useArticleCatalog(contentRef);
		return { contentRef, ...catalog };
	},
	template: '<div ref="contentRef"><h2>窗口标题</h2></div>',
});

const DialogCatalogHarness = defineComponent({
	setup() {
		const contentRef = ref<HTMLElement | null>(null);
		const scrollTarget = ref<HTMLElement | null>(null);
		const headingOffset = ref(120);
		const catalog = useArticleCatalog(contentRef, { scrollTarget, headingOffset });
		return { contentRef, scrollTarget, headingOffset, ...catalog };
	},
	template:
		'<div ref="scrollTarget"><div ref="contentRef"><h2 id="shared-heading">预览章节</h2><h3>预览子节</h3></div></div>',
});

afterEach(() => {
	document.body.innerHTML = '';
	vi.restoreAllMocks();
});

describe('useArticleCatalog', () => {
	it('builds a catalog with stable unique heading ids', () => {
		const wrapper = mount(CatalogHarness, { attachTo: document.body });

		wrapper.vm.refreshCatalog();

		expect(wrapper.vm.catalogList).toEqual([
			{ id: '重复-标题', text: '重复 标题！', level: 2 },
			{ id: '重复-标题-2', text: '重复 标题！', level: 3 },
			{ id: 'custom-id', text: '自定义标题', level: 4 },
		]);
		expect(wrapper.findAll('h2, h3, h4').map((heading) => heading.attributes('id'))).toEqual([
			'重复-标题',
			'重复-标题-2',
			'custom-id',
		]);
	});

	it('scrolls the nearest element scroll container to the selected heading', () => {
		const wrapper = mount(CatalogHarness, { attachTo: document.body });
		const container = wrapper.get<HTMLElement>('.scroll-container').element;
		const heading = wrapper.get<HTMLElement>('h2').element;
		const getComputedStyle = window.getComputedStyle.bind(window);
		vi.spyOn(window, 'getComputedStyle').mockImplementation((element, pseudoElement) => {
			if (element === container) {
				return { overflowY: 'auto' } as CSSStyleDeclaration;
			}

			return getComputedStyle(element, pseudoElement);
		});
		container.scrollTo = vi.fn();
		Object.defineProperty(container, 'scrollTop', { configurable: true, value: 50 });
		Object.defineProperty(container, 'clientHeight', { configurable: true, value: 500 });
		Object.defineProperty(container, 'scrollHeight', { configurable: true, value: 1500 });
		vi.spyOn(container, 'getBoundingClientRect').mockReturnValue({
			top: 10,
		} as DOMRect);
		vi.spyOn(heading, 'getBoundingClientRect').mockReturnValue({
			top: 210,
		} as DOMRect);

		wrapper.vm.refreshCatalog();
		wrapper.vm.scrollToHeading('重复-标题');

		expect(container.scrollTo).toHaveBeenCalledWith({
			top: 210 - 10 + 50 - ACTIVE_HEADING_OFFSET,
			behavior: 'smooth',
		});
	});

	it('falls back to window scrolling when no element scroll container exists', () => {
		const wrapper = mount(WindowCatalogHarness, { attachTo: document.body });
		const heading = wrapper.get<HTMLElement>('h2').element;
		heading.scrollIntoView = vi.fn();

		wrapper.vm.refreshCatalog();
		wrapper.vm.scrollToHeading('窗口标题');

		expect(heading.scrollIntoView).toHaveBeenCalledWith({
			behavior: 'smooth',
			block: 'start',
		});
	});

	it('clears catalog state when content is unavailable', () => {
		const wrapper = mount(WindowCatalogHarness);
		wrapper.vm.refreshCatalog();
		expect(wrapper.vm.catalogList).toHaveLength(1);

		wrapper.vm.contentRef = null;
		wrapper.vm.refreshCatalog();

		expect(wrapper.vm.catalogList).toEqual([]);
		expect(wrapper.vm.activeCatalogId).toBe('');
	});

	it('uses the explicit dialog container and header offset for scoped jumps and active headings', () => {
		const outsideHeading = document.createElement('h2');
		outsideHeading.id = 'shared-heading';
		document.body.appendChild(outsideHeading);
		const outsideRect = vi.spyOn(outsideHeading, 'getBoundingClientRect');
		const wrapper = mount(DialogCatalogHarness, { attachTo: document.body });
		const dialog = wrapper.vm.scrollTarget!;
		let scrollTop = 50;
		Object.defineProperties(dialog, {
			scrollTop: { configurable: true, get: () => scrollTop },
			clientHeight: { configurable: true, value: 500 },
			scrollHeight: { configurable: true, value: 1500 },
		});
		dialog.scrollTo = vi.fn();
		vi.spyOn(dialog, 'getBoundingClientRect').mockReturnValue({ top: 10 } as DOMRect);
		const headings = wrapper.findAll('h2, h3');
		vi.spyOn(headings[0].element, 'getBoundingClientRect').mockImplementation(
			() => ({ top: 260 - scrollTop }) as DOMRect,
		);
		vi.spyOn(headings[1].element, 'getBoundingClientRect').mockImplementation(
			() => ({ top: 710 - scrollTop }) as DOMRect,
		);
		let frame: FrameRequestCallback | undefined;
		vi.spyOn(window, 'requestAnimationFrame').mockImplementation((callback) => {
			frame = callback;
			return 1;
		});

		wrapper.vm.refreshCatalog();
		expect(wrapper.vm.activeCatalogId).toBe('');
		wrapper.vm.scrollToHeading('shared-heading');
		expect(dialog.scrollTo).toHaveBeenCalledWith({ top: 130, behavior: 'smooth' });
		expect(outsideRect).not.toHaveBeenCalled();
		scrollTop = 150;
		dialog.dispatchEvent(new Event('scroll'));
		frame!(0);
		expect(wrapper.vm.activeCatalogId).toBe('shared-heading');

		const removeListener = vi.spyOn(dialog, 'removeEventListener');
		wrapper.vm.contentRef = null;
		wrapper.vm.refreshCatalog();
		expect(removeListener).toHaveBeenCalledWith('scroll', expect.any(Function));
		expect(wrapper.vm.catalogList).toEqual([]);
		wrapper.unmount();
	});
});
