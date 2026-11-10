import { mount, flushPromises } from '@vue/test-utils';
import { defineComponent, ref } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import PublicCommentReplyEditor from './PublicCommentReplyEditor.vue';

const ElInputStub = defineComponent({
	setup(_, { expose }) {
		const textarea = ref<HTMLTextAreaElement>();
		expose({ textarea });
		return { textarea };
	},
	template: '<textarea ref="textarea" />',
});

afterEach(() => vi.restoreAllMocks());

describe('PublicCommentReplyEditor', () => {
	it('updates content and emits cancel and submit events', async () => {
		const wrapper = mount(PublicCommentReplyEditor, {
			props: {
				modelValue: '',
				targetName: '测试用户',
				loading: false,
			},
			global: {
				stubs: {
					ElInput: {
						props: ['modelValue'],
						template:
							'<textarea :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
					},
					ElButton: {
						template: '<button><slot /></button>',
					},
				},
			},
		});

		expect(wrapper.text()).toContain('回复 测试用户：');
		await wrapper.get('textarea').setValue('回复内容');
		expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['回复内容']);

		const buttons = wrapper.findAll('button');
		await buttons[0].trigger('click');
		await buttons[1].trigger('click');
		expect(wrapper.emitted('cancel')).toHaveLength(1);
		expect(wrapper.emitted('submit')).toHaveLength(1);
	});

	it.each([true, false])('focuses only when autofocus is %s', async (autofocus) => {
		const focus = vi.spyOn(HTMLTextAreaElement.prototype, 'focus');
		const wrapper = mount(PublicCommentReplyEditor, {
			props: { modelValue: '', targetName: '管理员', loading: false, autofocus },
			global: { stubs: { ElInput: ElInputStub, ElButton: true, ElCheckbox: true } },
		});
		await flushPromises();
		if (autofocus) {
			expect(focus).toHaveBeenCalledExactlyOnceWith({ preventScroll: true });
		} else {
			expect(focus).not.toHaveBeenCalled();
		}
		await wrapper.setProps({ loading: true });
		expect(focus).toHaveBeenCalledTimes(autofocus ? 1 : 0);
		wrapper.unmount();
	});
});
