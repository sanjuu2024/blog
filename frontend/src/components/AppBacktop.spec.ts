import { mount } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AppBacktop from './AppBacktop.vue';

afterEach(() => {
	document.body.innerHTML = '';
});

describe('AppBacktop', () => {
	it('scrolls the application layout to the top', async () => {
		const scrollContainer = document.createElement('div');
		scrollContainer.className = 'app-layout';
		scrollContainer.scrollTo = vi.fn();
		document.body.append(scrollContainer);

		const wrapper = mount(AppBacktop, {
			global: {
				stubs: {
					ILucideArrowUp: true,
				},
			},
		});

		await wrapper.get('button').trigger('click');

		expect(scrollContainer.scrollTo).toHaveBeenCalledWith({
			top: 0,
			behavior: 'smooth',
		});
	});
});
