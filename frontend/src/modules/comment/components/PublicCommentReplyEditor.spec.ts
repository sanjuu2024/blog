import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import PublicCommentReplyEditor from './PublicCommentReplyEditor.vue';

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
});
