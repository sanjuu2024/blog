import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import StickerImage from '@/components/sticker/StickerImage.vue';

describe('StickerImage', () => {
	it('keeps the lazy image mounted under an animated skeleton and forwards its load event', async () => {
		const wrapper = mount(StickerImage, {
			props: { name: 'doge', url: '/stickers/doge.png', minHeightRem: 2.5 },
		});
		const image = wrapper.get('img');
		expect(image.attributes('loading')).toBe('lazy');
		expect(image.attributes('decoding')).toBe('async');
		expect(wrapper.get('.sticker-image').attributes('aria-busy')).toBe('true');
		expect(wrapper.get('.sticker-image').attributes('style')).toContain(
			'--sticker-min-height: 2.5rem',
		);
		expect(wrapper.get('.sticker-image__skeleton').classes()).toContain('is-animated');
		expect(wrapper.findAll('.el-skeleton__item')).toHaveLength(1);
		expect(wrapper.get('.sticker-image__placeholder').classes()).toContain('el-skeleton__rect');
		await image.trigger('load');
		expect(wrapper.find('.sticker-image__skeleton').exists()).toBe(false);
		expect(wrapper.get('.sticker-image').classes()).not.toContain('sticker-image--loading');
		expect(wrapper.get('.sticker-image').attributes('aria-busy')).toBe('false');
		expect((wrapper.emitted('load')?.[0]?.[0] as Event).target).toBe(image.element);
		wrapper.unmount();
	});

	it('renders original resources and falls back to text on failure', async () => {
		const wrapper = mount(StickerImage, {
			props: { name: 'doge', url: '/stickers/doge.png' },
		});
		expect(wrapper.get('img').attributes('alt')).toBe('[doge]');
		await wrapper.get('img').trigger('error');
		expect(wrapper.find('img').exists()).toBe(false);
		expect(wrapper.find('.sticker-image__skeleton').exists()).toBe(false);
		expect(wrapper.text()).toBe('[doge]');
		await wrapper.setProps({ url: '/stickers/new.png' });
		expect(wrapper.find('img').exists()).toBe(true);
		expect(wrapper.find('.sticker-image__skeleton').exists()).toBe(true);
		await wrapper.get('img').trigger('load');
		expect(wrapper.find('.sticker-image__skeleton').exists()).toBe(false);
		wrapper.unmount();
	});

	it('resets loading on URL changes and ignores events from the previous image', async () => {
		const wrapper = mount(StickerImage, {
			props: { name: 'doge', url: '/stickers/doge.png' },
		});
		const previousImage = wrapper.get('img');
		await previousImage.trigger('load');
		await wrapper.setProps({ url: '/stickers/new.png' });
		expect(wrapper.get('img').element).not.toBe(previousImage.element);
		expect(wrapper.find('.sticker-image__skeleton').exists()).toBe(true);
		await previousImage.trigger('load');
		await previousImage.trigger('error');
		expect(wrapper.find('.sticker-image__skeleton').exists()).toBe(true);
		expect(wrapper.get('img').attributes('src')).toBe('/stickers/new.png');
		expect(wrapper.emitted('load')).toHaveLength(1);
		await wrapper.get('img').trigger('load');
		expect(wrapper.find('.sticker-image__skeleton').exists()).toBe(false);
		expect(wrapper.emitted('load')).toHaveLength(2);
		wrapper.unmount();
	});
});
