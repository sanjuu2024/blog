import { mount } from '@vue/test-utils';
import { defineComponent, nextTick, ref } from 'vue';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useArticleCommentAnchor } from './useArticleCommentAnchor';

let resized: ResizeObserverCallback;
const disconnect = vi.fn();
const Harness = defineComponent({
	setup() {
		const pageRef = ref<HTMLElement | null>(null);
		return { pageRef, ...useArticleCommentAnchor(pageRef) };
	},
	template: '<div class="app-layout"><div ref="pageRef"><img /><section /></div></div>',
});

function mountAnchor() {
	const wrapper = mount(Harness, { attachTo: document.body });
	const container = wrapper.get<HTMLElement>('.app-layout').element;
	const target = wrapper.get<HTMLElement>('section').element;
	let targetTop = 600;
	vi.spyOn(container, 'getBoundingClientRect').mockReturnValue({ top: 20 } as DOMRect);
	vi.spyOn(target, 'getBoundingClientRect').mockImplementation(
		() => ({ top: targetTop + 20 - container.scrollTop }) as DOMRect,
	);
	target.style.scrollMarginTop = '80px';
	container.scrollTo = vi.fn((options?: ScrollToOptions | number) => {
		if (typeof options === 'object') container.scrollTop = options.top ?? 0;
	}) as typeof container.scrollTo;
	return { wrapper, container, target, moveTarget: (top: number) => (targetTop = top) };
}

describe('useArticleCommentAnchor', () => {
	beforeEach(() => {
		vi.useFakeTimers();
		vi.clearAllMocks();
		vi.stubGlobal(
			'ResizeObserver',
			class {
				constructor(callback: ResizeObserverCallback) {
					resized = callback;
				}
				observe = vi.fn();
				unobserve = vi.fn();
				disconnect = disconnect;
			},
		);
	});

	afterEach(() => {
		vi.useRealTimers();
		vi.unstubAllGlobals();
		vi.restoreAllMocks();
		document.body.innerHTML = '';
	});

	it('corrects the actual scroll container after images or comments change its height', async () => {
		const { wrapper, container, target, moveTarget } = mountAnchor();
		await wrapper.vm.locate(target);
		expect(container.scrollTo).toHaveBeenLastCalledWith({ top: 520, behavior: 'instant' });
		moveTarget(1100);
		resized([], {} as ResizeObserver);
		expect(container.scrollTo).toHaveBeenLastCalledWith({ top: 1020, behavior: 'instant' });
		wrapper.unmount();
	});

	it.each(['wheel', 'touchstart', 'pointerdown', 'keydown'])(
		'stops correcting after the user sends %s input',
		async (event) => {
			const { wrapper, container, target, moveTarget } = mountAnchor();
			await wrapper.vm.locate(target);
			document.dispatchEvent(new Event(event));
			await nextTick();
			moveTarget(1100);
			resized([], {} as ResizeObserver);
			expect(container.scrollTo).toHaveBeenCalledTimes(1);
			expect(disconnect).toHaveBeenCalled();
			wrapper.unmount();
		},
	);

	it('stops after the deadline and when navigation cancels the target', async () => {
		const { wrapper, container, target } = mountAnchor();
		await wrapper.vm.locate(target);
		await vi.advanceTimersByTimeAsync(10_000);
		resized([], {} as ResizeObserver);
		expect(container.scrollTo).toHaveBeenCalledTimes(1);
		await wrapper.vm.locate(target);
		wrapper.vm.stop();
		resized([], {} as ResizeObserver);
		expect(container.scrollTo).toHaveBeenCalledTimes(2);
		wrapper.unmount();
	});

	it('disconnects observation on unmount', async () => {
		const { wrapper, target } = mountAnchor();
		await wrapper.vm.locate(target);
		wrapper.unmount();
		expect(disconnect).toHaveBeenCalled();
	});
});
