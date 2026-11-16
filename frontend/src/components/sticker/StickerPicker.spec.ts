import { mount } from '@vue/test-utils';
import { ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import StickerPicker from '@/components/sticker/StickerPicker.vue';

const state = vi.hoisted(() => ({ loading: false, failed: false, loadStickers: vi.fn() }));

vi.mock('@/composables/useStickers', () => ({
	useStickers: () => ({
		packs: ref([
			{
				id: 'first',
				name: '第一个库',
				iconUrl: '/first.png',
				items: [{ token: '[sticker:first:one]', name: 'one', url: '/one.png' }],
			},
			{
				id: 'second',
				name: '第二个库',
				iconUrl: '/second.gif',
				minHeightRem: 2.5,
				items: [{ token: '[sticker:second:two]', name: 'two', url: '/two.gif' }],
			},
		]),
		loading: ref(state.loading),
		failed: ref(state.failed),
		loadStickers: state.loadStickers,
	}),
}));

describe('StickerPicker', () => {
	beforeEach(() => {
		state.loading = false;
		state.failed = false;
		state.loadStickers.mockReset();
	});

	it('uses the shared loading component while the catalog request is pending', () => {
		state.loading = true;
		const wrapper = mount(StickerPicker, { global: { stubs: { ILucideX: true } } });
		expect(wrapper.find('.app-loading__image').exists()).toBe(true);
		expect(wrapper.find('.sticker-picker__items').exists()).toBe(false);
		expect(wrapper.text()).not.toContain('正在加载表情');
		wrapper.unmount();
	});

	it('keeps a retry action when the catalog fails to load', async () => {
		state.failed = true;
		const wrapper = mount(StickerPicker, {
			global: {
				stubs: { ILucideX: true, ElButton: { template: '<button><slot /></button>' } },
			},
		});
		expect(wrapper.find('.app-loading__image').exists()).toBe(false);
		expect(wrapper.text()).toContain('表情加载失败');
		await wrapper.get('.sticker-picker__state button').trigger('click');
		expect(state.loadStickers).toHaveBeenCalledOnce();
		wrapper.unmount();
	});

	it('switches packs, selects their tokens and exposes a close action', async () => {
		const wrapper = mount(StickerPicker, {
			global: { stubs: { StickerImage: true, ILucideX: true } },
		});
		expect(wrapper.text()).toContain('第一个库');
		expect(wrapper.text()).toContain('1 个');
		expect(
			(wrapper.get('.sticker-picker__items').element as HTMLElement).style.getPropertyValue(
				'--sticker-min-height',
			),
		).toBe('1.5rem');
		await wrapper.get('[aria-label="第二个库"]').trigger('click');
		expect(
			(wrapper.get('.sticker-picker__items').element as HTMLElement).style.getPropertyValue(
				'--sticker-min-height',
			),
		).toBe('2.5rem');
		expect(wrapper.get('[aria-label="第二个库"]').attributes('aria-pressed')).toBe('true');
		await wrapper.get('[aria-label="插入表情：two"]').trigger('click');
		expect(wrapper.emitted('select')?.[0]?.[0]).toMatchObject({
			token: '[sticker:second:two]',
		});
		await wrapper.get('[aria-label="关闭表情选择器"]').trigger('click');
		expect(wrapper.emitted('close')).toHaveLength(1);
	});

	it('resets the image size when switching back to a pack without configuration', async () => {
		const wrapper = mount(StickerPicker, { global: { stubs: { ILucideX: true } } });
		await wrapper.get('[aria-label="第二个库"]').trigger('click');
		expect(wrapper.get('.sticker-picker__items').attributes('style')).toContain('2.5rem');
		expect(wrapper.get('[aria-label="插入表情：two"] img').attributes('src')).toBe('/two.gif');
		expect(wrapper.get('[aria-label="第二个库"] img').attributes('style')).toBeUndefined();
		await wrapper.get('[aria-label="第一个库"]').trigger('click');
		expect(wrapper.get('.sticker-picker__items').attributes('style')).toContain('1.5rem');
		expect(wrapper.get('[aria-label="插入表情：one"] img').attributes('src')).toBe('/one.png');
	});

	it('fits wide stickers at the pack height and resets the measured ratio on switching', async () => {
		const wrapper = mount(StickerPicker, { global: { stubs: { ILucideX: true } } });
		const image = wrapper.get('[aria-label="插入表情：one"] img');
		Object.defineProperties(image.element, {
			naturalWidth: { value: 180 },
			naturalHeight: { value: 60 },
		});
		await image.trigger('load');
		const style = () => (wrapper.get('.sticker-picker__items').element as HTMLElement).style;
		expect(style().getPropertyValue('--sticker-min-height')).toBe('1.5rem');
		expect(style().getPropertyValue('--sticker-cell-min-width')).toBe('6rem');
		await wrapper.get('[aria-label="第二个库"]').trigger('click');
		expect(style().getPropertyValue('--sticker-min-height')).toBe('2.5rem');
		expect(style().getPropertyValue('--sticker-cell-min-width')).toBe('4rem');
	});

	it('allows choosing a loaded sticker while another sticker image is still loading', async () => {
		const wrapper = mount(StickerPicker, { global: { stubs: { ILucideX: true } } });
		const item = wrapper.get('[aria-label="插入表情：one"]');
		await item.get('img').trigger('load');
		expect(item.find('.sticker-image__skeleton').exists()).toBe(false);
		expect(
			wrapper.get('[aria-label="第一个库"]').find('.sticker-image__skeleton').exists(),
		).toBe(true);
		await item.trigger('click');
		expect(wrapper.emitted('select')?.[0]?.[0]).toMatchObject({ token: '[sticker:first:one]' });
		wrapper.unmount();
	});
});
