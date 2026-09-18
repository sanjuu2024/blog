import { mount } from '@vue/test-utils';
import { defineComponent, ref } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useArticleCatalog } from './useArticleCatalog';

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
			top: 154,
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
});
