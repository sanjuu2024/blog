import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent, ref } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import CommentTextInput from './CommentTextInput.vue';

const token = '[sticker:bilibili:doge]';
const sticker = { token, name: 'doge', url: '/stickers/bilibili/bb_doge.png' };
vi.mock('../composables/useCommentStickers', () => ({
	useCommentStickers: () => ({ stickers: ref(new Map([[token, sticker]])) }),
}));

const ElInput = defineComponent({
	props: ['modelValue'],
	emits: ['update:modelValue'],
	setup(_, { expose }) {
		const textarea = ref<HTMLTextAreaElement>();
		expose({ textarea });
		return { textarea };
	},
	template:
		'<textarea ref="textarea" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
});

describe('CommentTextInput', () => {
	it('inserts at the previous selection, keeps the picker open and restores the caret', async () => {
		const wrapper = mount(CommentTextInput, {
			props: {
				modelValue: '前选中文后',
				'onUpdate:modelValue': (value: string) => wrapper.setProps({ modelValue: value }),
			},
			global: {
				stubs: { ElInput, CommentStickerPicker: true, ISolarEmojiFunnyCircleOutline: true },
			},
		});
		const textarea = wrapper.get('textarea');
		(textarea.element as HTMLTextAreaElement).setSelectionRange(1, 4);
		await textarea.trigger('select');
		await wrapper.get('[aria-label="选择表情"]').trigger('click');
		wrapper.getComponent({ name: 'CommentStickerPicker' }).vm.$emit('select', sticker);
		await flushPromises();
		expect(wrapper.props('modelValue')).toBe(`前${token}后`);
		expect((textarea.element as HTMLTextAreaElement).selectionStart).toBe(token.length + 1);
		expect(wrapper.find('comment-sticker-picker-stub').exists()).toBe(true);
		expect(wrapper.text()).toContain('3 / 1000');
		wrapper.unmount();
	});

	it('allows 1000 display characters including 20 stickers within the raw limit', () => {
		const wrapper = mount(CommentTextInput, {
			props: { modelValue: '字'.repeat(980) + token.repeat(20) },
			global: {
				stubs: { ElInput, CommentStickerPicker: true, ISolarEmojiFunnyCircleOutline: true },
			},
		});
		expect(wrapper.text()).toContain('1000 / 1000');
		expect(wrapper.text()).toContain('表情 20 / 20');
		expect(wrapper.get('textarea').attributes('maxlength')).toBe('2260');
	});

	it('blocks a twenty-first sticker but permits replacing selected stickers at the limit', async () => {
		const wrapper = mount(CommentTextInput, {
			props: {
				modelValue: token.repeat(20),
				'onUpdate:modelValue': (value: string) => wrapper.setProps({ modelValue: value }),
			},
			global: {
				stubs: { ElInput, CommentStickerPicker: true, ISolarEmojiFunnyCircleOutline: true },
			},
		});
		const textarea = wrapper.get('textarea');
		(textarea.element as HTMLTextAreaElement).setSelectionRange(
			token.length * 20,
			token.length * 20,
		);
		await wrapper.get('[aria-label="选择表情"]').trigger('click');
		wrapper.getComponent({ name: 'CommentStickerPicker' }).vm.$emit('select', sticker);
		await flushPromises();
		expect(wrapper.emitted('update:modelValue')).toBeUndefined();
		(textarea.element as HTMLTextAreaElement).setSelectionRange(0, token.length * 2);
		await textarea.trigger('select');
		wrapper.getComponent({ name: 'CommentStickerPicker' }).vm.$emit('select', sticker);
		await flushPromises();
		expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([token.repeat(19)]);
	});

	it('marks manually entered or pasted content with too many stickers as invalid', () => {
		const wrapper = mount(CommentTextInput, {
			props: { modelValue: token.repeat(21) },
			global: {
				stubs: { ElInput, CommentStickerPicker: true, ISolarEmojiFunnyCircleOutline: true },
			},
		});
		expect(wrapper.get('.comment-text-input__count--invalid').text()).toBe('表情 21 / 20');
	});
});
