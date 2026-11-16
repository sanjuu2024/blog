import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const getStickers = vi.hoisted(() => vi.fn());
vi.mock('@/api/stickerApi', () => ({ getStickers: getStickers }));
vi.mock('@/components/sticker/StickerImage.vue', async () => {
	const { defineComponent } = await import('vue');
	return {
		default: defineComponent({
			name: 'StickerImage',
			props: ['name', 'url', 'minHeightRem'],
			template: '<img :src="url" />',
		}),
	};
});

beforeEach(() => {
	vi.resetModules();
	getStickers.mockReset();
});

describe('useStickers', () => {
	it('shares one request and caches the successful catalog across editors', async () => {
		const { useStickers } = await import('./useStickers');
		getStickers.mockResolvedValue([
			{ id: 'test', items: [{ token: 'token', name: 'name', url: '/test.png' }] },
		]);
		const component = defineComponent({ setup: useStickers, template: '<div />' });
		const first = mount(component);
		const second = mount(component);
		await flushPromises();
		mount(component);
		await flushPromises();
		expect(getStickers).toHaveBeenCalledTimes(1);
		expect(first.vm.stickers.has('token')).toBe(true);
		first.unmount();
		second.unmount();
	});

	it('keeps a failure state and permits an explicit retry', async () => {
		const { useStickers } = await import('./useStickers');
		getStickers.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce([]);
		const wrapper = mount(defineComponent({ setup: useStickers, template: '<div />' }));
		await flushPromises();
		expect(wrapper.vm.failed).toBe(true);
		await wrapper.vm.loadStickers();
		expect(wrapper.vm.failed).toBe(false);
		expect(getStickers).toHaveBeenCalledTimes(2);
	});

	it('applies each pack height to its tokens when rendering mixed content', async () => {
		const { default: StickerContent } = await import('@/components/sticker/StickerContent.vue');
		getStickers.mockResolvedValue([
			{
				id: 'small',
				items: [{ token: '[sticker:small:one]', name: 'one', url: '/one.png' }],
			},
			{
				id: 'large',
				minHeightRem: 2.5,
				items: [{ token: '[sticker:large:two]', name: 'two', url: '/two.gif' }],
			},
		]);
		const wrapper = mount(StickerContent, {
			props: { content: '正文[sticker:small:one][sticker:large:two][sticker:unknown:three]' },
		});
		await flushPromises();
		const images = wrapper.findAllComponents({ name: 'StickerImage' });
		expect(images).toHaveLength(2);
		expect(images[0]?.props()).toMatchObject({ url: '/one.png', minHeightRem: undefined });
		expect(images[1]?.props()).toMatchObject({ url: '/two.gif', minHeightRem: 2.5 });
		expect(wrapper.text()).toBe('正文[sticker:unknown:three]');
		wrapper.unmount();
	});
});
