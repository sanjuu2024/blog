import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import AppLoadMoreTrigger from './AppLoadMoreTrigger.vue';

vi.mock('@vueuse/core', () => ({
	useIntersectionObserver: vi.fn(),
}));

describe('AppLoadMoreTrigger', () => {
	it('can hide the end-of-list message when the list has no records', () => {
		const wrapper = mount(AppLoadMoreTrigger, {
			props: {
				hasNext: false,
				thingStr: '留言',
				showNoMore: false,
			},
			global: {
				stubs: { ILucideLoader: true },
			},
		});

		expect(wrapper.text()).toBe('');
	});

	it('shows the end-of-list message by default', () => {
		const wrapper = mount(AppLoadMoreTrigger, {
			props: { hasNext: false, thingStr: '留言' },
			global: {
				stubs: { ILucideLoader: true },
			},
		});

		expect(wrapper.text()).toBe('没有更多留言了');
	});
});
