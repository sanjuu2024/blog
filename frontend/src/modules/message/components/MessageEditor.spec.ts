import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import MessageEditor from './MessageEditor.vue';

function mountEditor(isLogin = false) {
	return mount(MessageEditor, {
		props: { isLogin },
		global: {
			stubs: {
				ElInput: {
					props: ['modelValue'],
					template:
						'<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
				},
				ElCheckbox: {
					props: ['modelValue'],
					template:
						'<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />',
				},
				ElButton: {
					props: ['disabled'],
					template: '<button :disabled="disabled"><slot /></button>',
				},
				ILucideSend: true,
				ILucideUserRound: true,
			},
		},
	});
}

describe('MessageEditor', () => {
	it('requires guest nickname before submitting', async () => {
		const wrapper = mountEditor();
		const inputs = wrapper.findAll('input');
		const button = wrapper.find('button').element as HTMLButtonElement;
		await inputs[3].setValue('留言内容');

		expect(button.disabled).toBe(true);

		await inputs[0].setValue('访客');
		expect(button.disabled).toBe(false);
	});

	it('requires guest email only when notification is enabled', async () => {
		const wrapper = mountEditor();
		const inputs = wrapper.findAll('input');
		const button = wrapper.find('button').element as HTMLButtonElement;
		await inputs[0].setValue('访客');
		await inputs[3].setValue('留言内容');
		await inputs[2].setValue(true);

		expect(button.disabled).toBe(true);

		await inputs[1].setValue('guest@example.com');
		expect(button.disabled).toBe(false);
	});

	it('only requires content for logged-in users', async () => {
		const wrapper = mountEditor(true);
		const inputs = wrapper.findAll('input');
		const button = wrapper.find('button').element as HTMLButtonElement;

		expect(button.disabled).toBe(true);
		await inputs[1].setValue('登录用户留言');
		expect(button.disabled).toBe(false);
	});
});
