import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const getStickers = vi.hoisted(() => vi.fn());
vi.mock('@/api/stickerApi', () => ({ getStickers: getStickers }));

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
});
