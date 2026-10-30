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
	const target = wrapper.get<HTMLElement>('section').element;
	const scrollIntoView = vi.fn();
	target.scrollIntoView = scrollIntoView;
	return { wrapper, target, scrollIntoView };
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

	it('realigns the target after images or comments change the page height', async () => {
		const { wrapper, target, scrollIntoView } = mountAnchor();
		await wrapper.vm.locate(target);
		expect(scrollIntoView).toHaveBeenCalledExactlyOnceWith({
			block: 'start',
			behavior: 'instant',
		});
		resized([], {} as ResizeObserver);
		expect(scrollIntoView).toHaveBeenCalledTimes(2);
		expect(scrollIntoView).toHaveBeenLastCalledWith({ block: 'start', behavior: 'instant' });
		wrapper.unmount();
	});

	it.each(['wheel', 'touchstart', 'pointerdown', 'keydown'])(
		'stops correcting after the user sends %s input',
		async (event) => {
			const { wrapper, target, scrollIntoView } = mountAnchor();
			await wrapper.vm.locate(target);
			document.dispatchEvent(new Event(event));
			await nextTick();
			resized([], {} as ResizeObserver);
			expect(scrollIntoView).toHaveBeenCalledTimes(1);
			expect(disconnect).toHaveBeenCalled();
			wrapper.unmount();
		},
	);

	it('stops after the deadline and when navigation cancels the target', async () => {
		const { wrapper, target, scrollIntoView } = mountAnchor();
		await wrapper.vm.locate(target);
		await vi.advanceTimersByTimeAsync(10_000);
		resized([], {} as ResizeObserver);
		expect(scrollIntoView).toHaveBeenCalledTimes(1);
		await wrapper.vm.locate(target);
		wrapper.vm.stop();
		resized([], {} as ResizeObserver);
		expect(scrollIntoView).toHaveBeenCalledTimes(2);
		wrapper.unmount();
	});

	it('disconnects observation on unmount', async () => {
		const { wrapper, target } = mountAnchor();
		await wrapper.vm.locate(target);
		wrapper.unmount();
		expect(disconnect).toHaveBeenCalled();
	});
});
