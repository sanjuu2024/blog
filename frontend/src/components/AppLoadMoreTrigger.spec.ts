import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import AppLoadMoreTrigger from './AppLoadMoreTrigger.vue';
import AppLoading from './AppLoading.vue';

vi.mock('@vueuse/core', () => ({
	useIntersectionObserver: vi.fn(),
}));

describe('AppLoadMoreTrigger', () => {
	it('reuses AppLoading, falls back on error and retries on the next load', async () => {
		const wrapper = mount(AppLoadMoreTrigger, {
			props: { loading: true, hasNext: true },
			global: { stubs: { ILucideLoader: true } },
		});

		expect(wrapper.get('img').isVisible()).toBe(true);
		expect(wrapper.getComponent(AppLoading).props('loading')).toBe(true);
		expect(wrapper.find('.animate-spin').exists()).toBe(false);
		expect(wrapper.text()).toBe('Loading......');
		await wrapper.get('img').trigger('error');
		expect(wrapper.find('img').exists()).toBe(false);
		expect(wrapper.find('.animate-spin').exists()).toBe(true);
		await wrapper.setProps({ loading: false });
		expect(wrapper.find('img').exists()).toBe(false);
		expect(wrapper.getComponent(AppLoading).props('loading')).toBe(false);
		await wrapper.setProps({ loading: true });
		expect(wrapper.get('img').isVisible()).toBe(true);
		expect(wrapper.find('.animate-spin').exists()).toBe(false);
	});

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
