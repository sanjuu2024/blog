import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import StickerImage from '@/components/sticker/StickerImage.vue';

describe('StickerImage', () => {
	it('renders original resources and falls back to text on failure', async () => {
		const wrapper = mount(StickerImage, {
			props: { name: 'doge', url: '/stickers/doge.png' },
		});
		expect(wrapper.get('img').attributes('alt')).toBe('[doge]');
		await wrapper.get('img').trigger('error');
		expect(wrapper.find('img').exists()).toBe(false);
		expect(wrapper.text()).toBe('[doge]');
		await wrapper.setProps({ url: '/stickers/new.png' });
		expect(wrapper.find('img').exists()).toBe(true);
	});
});
