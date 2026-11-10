import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import AppFixedButton from './AppFixedButton.vue';

describe('AppFixedButton', () => {
	it('renders slot content and forwards native attributes to a regular button', () => {
		const wrapper = mount(AppFixedButton, {
			attrs: {
				class: 'custom-button',
				'aria-label': '打开目录',
				'aria-expanded': 'false',
				'aria-controls': 'article-catalog',
			},
			slots: { default: '<span>目录</span>' },
		});
		const button = wrapper.get('button');

		expect(button.text()).toBe('目录');
		expect(button.attributes()).toMatchObject({
			type: 'button',
			'aria-label': '打开目录',
			'aria-expanded': 'false',
			'aria-controls': 'article-catalog',
		});
		expect(button.classes()).toContain('custom-button');
		wrapper.unmount();
	});

	it('updates height, placement and pixel offsets without adding a wrapper element', async () => {
		const wrapper = mount(AppFixedButton);
		const button = wrapper.get('button');

		expect(wrapper.element).toBe(button.element);
		expect(button.element.style.height).toBe('44px');
		expect(button.classes()).toContain('app-fixed-button--bottom-right');
		expect(button.element.style.getPropertyValue('--app-fixed-button-offset-x')).toBe('32px');
		expect(button.element.style.getPropertyValue('--app-fixed-button-offset-y')).toBe('32px');

		await wrapper.setProps({
			placement: 'bottom-left',
			offsetX: 16,
			offsetY: 100,
			heightMultiplier: 1.5,
		});

		expect(button.element.style.height).toBe('66px');
		expect(button.classes()).toContain('app-fixed-button--bottom-left');
		expect(button.classes()).not.toContain('app-fixed-button--bottom-right');
		expect(button.element.style.getPropertyValue('--app-fixed-button-offset-x')).toBe('16px');
		expect(button.element.style.getPropertyValue('--app-fixed-button-offset-y')).toBe('100px');
		await wrapper.setProps({ heightMultiplier: 1 });
		expect(button.element.style.height).toBe('44px');
		wrapper.unmount();
	});

	it('forwards the original mouse event exactly once', () => {
		const onClick = vi.fn();
		const wrapper = mount(AppFixedButton, { props: { onClick } });
		const event = new MouseEvent('click', { bubbles: true, clientX: 40, clientY: 300 });

		wrapper.get('button').element.dispatchEvent(event);

		expect(onClick).toHaveBeenCalledExactlyOnceWith(event);
		expect(wrapper.emitted('click')).toEqual([[event]]);
		wrapper.unmount();
	});

	it('respects the native disabled attribute', async () => {
		const onClick = vi.fn();
		const wrapper = mount(AppFixedButton, { attrs: { disabled: true }, props: { onClick } });

		expect(wrapper.get('button').element.disabled).toBe(true);
		await wrapper.get('button').trigger('click');
		expect(onClick).not.toHaveBeenCalled();
		expect(wrapper.emitted('click')).toBeUndefined();
		wrapper.unmount();
	});
});
