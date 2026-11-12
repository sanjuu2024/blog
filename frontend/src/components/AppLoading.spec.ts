import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import AppLoading from './AppLoading.vue';

describe('AppLoading', () => {
	it('shows the image without waiting for load and only falls back after an error', async () => {
		const wrapper = mount(AppLoading, {
			props: { loading: true },
			global: { stubs: { ILucideLoader: true } },
		});
		expect(wrapper.get('img').isVisible()).toBe(true);
		expect(wrapper.text()).toBe('Loading......');
		expect(wrapper.find('.animate-spin').exists()).toBe(false);
		await wrapper.setProps({ loading: false });
		await wrapper.setProps({ loading: true });
		expect(wrapper.get('img').isVisible()).toBe(true);
		expect(wrapper.find('.animate-spin').exists()).toBe(false);
		await wrapper.get('img').trigger('error');
		expect(wrapper.find('img').exists()).toBe(false);
		expect(wrapper.text()).toBe('');
		expect(wrapper.find('.animate-spin').exists()).toBe(true);
	});

	it('preserves content and custom loading slots and retries the image on the next load', async () => {
		const wrapper = mount(AppLoading, {
			props: { loading: false },
			slots: { default: '<p>已加载内容</p>' },
		});
		expect(wrapper.text()).toBe('已加载内容');
		await wrapper.setProps({ loading: true });
		await wrapper.get('img').trigger('error');
		await wrapper.setProps({ loading: false });
		await wrapper.setProps({ loading: true });
		expect(wrapper.get('img').isVisible()).toBe(true);
		expect(wrapper.find('.animate-spin').exists()).toBe(false);
		const custom = mount(AppLoading, {
			props: { loading: true },
			slots: { loading: '<p>自定义占位</p>' },
		});
		expect(custom.text()).toBe('自定义占位');
		expect(custom.find('img').exists()).toBe(false);
	});
});
