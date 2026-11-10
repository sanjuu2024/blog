import { mount } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AppBacktop from './AppBacktop.vue';

afterEach(() => {
	vi.restoreAllMocks();
	document.body.innerHTML = '';
});

describe('AppBacktop', () => {
	it('scrolls the browser page to the top', async () => {
		const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => {});

		const wrapper = mount(AppBacktop, {
			global: {
				stubs: {
					ILucideArrowUp: true,
				},
			},
		});

		await wrapper.get('button').trigger('click');

		expect(scrollTo).toHaveBeenCalledExactlyOnceWith({
			top: 0,
			behavior: 'smooth',
		});
		wrapper.unmount();
	});

	it('scrolls the supplied container without moving the browser page', async () => {
		const pageScroll = vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
		const target = document.createElement('div');
		const scrollTo = vi.fn();
		target.scrollTo = scrollTo;
		const wrapper = mount(AppBacktop, {
			props: { target },
			global: { stubs: { ILucideArrowUp: true } },
		});

		await wrapper.get('button').trigger('click');

		expect(scrollTo).toHaveBeenCalledExactlyOnceWith({ top: 0, behavior: 'smooth' });
		expect(pageScroll).not.toHaveBeenCalled();
		wrapper.unmount();
	});
});
