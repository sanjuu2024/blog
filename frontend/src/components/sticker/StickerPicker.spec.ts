import { mount } from '@vue/test-utils';
import { ref } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import StickerPicker from '@/components/sticker/StickerPicker.vue';

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
				gridMinWidthRem: 4.5,
				items: [{ token: '[sticker:second:two]', name: 'two', url: '/two.gif' }],
			},
		]),
		loading: ref(false),
		failed: ref(false),
		loadStickers: vi.fn(),
	}),
}));

describe('StickerPicker', () => {
	it('switches packs, selects their tokens and exposes a close action', async () => {
		const wrapper = mount(StickerPicker, {
			global: { stubs: { StickerImage: true, ILucideX: true } },
		});
		expect(wrapper.text()).toContain('第一个库');
		expect(wrapper.text()).toContain('1 个');
		expect(
			(wrapper.get('.sticker-picker__items').element as HTMLElement).style.getPropertyValue(
				'--sticker-cell-min-width',
			),
		).toBe('3rem');
		await wrapper.get('[aria-label="第二个库"]').trigger('click');
		expect(
			(wrapper.get('.sticker-picker__items').element as HTMLElement).style.getPropertyValue(
				'--sticker-cell-min-width',
			),
		).toBe('4.5rem');
		expect(wrapper.get('[aria-label="第二个库"]').attributes('aria-pressed')).toBe('true');
		await wrapper.get('[aria-label="插入表情：two"]').trigger('click');
		expect(wrapper.emitted('select')?.[0]?.[0]).toMatchObject({
			token: '[sticker:second:two]',
		});
		await wrapper.get('[aria-label="关闭表情选择器"]').trigger('click');
		expect(wrapper.emitted('close')).toHaveLength(1);
	});
});
