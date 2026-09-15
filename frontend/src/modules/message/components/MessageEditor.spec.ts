import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MessageEditor from './MessageEditor.vue';

const mediaQueryState = vi.hoisted(() => ({ isMobile: false }));

vi.mock('@vueuse/core', async (importOriginal) => {
	const { computed } = await import('vue');
	return {
		...(await importOriginal<typeof import('@vueuse/core')>()),
		useMediaQuery: vi.fn(() => computed(() => mediaQueryState.isMobile)),
	};
});

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
	beforeEach(() => {
		mediaQueryState.isMobile = false;
	});

	it('requires guest nickname before submitting', async () => {
		const wrapper = mountEditor();
		const inputs = wrapper.findAll('input');
		const button = wrapper.get('.message-editor__body button').element as HTMLButtonElement;
		await inputs[3].setValue('留言内容');

		expect(button.disabled).toBe(true);

		await inputs[0].setValue('访客');
		expect(button.disabled).toBe(false);
	});

	it('requires guest email only when notification is enabled', async () => {
		const wrapper = mountEditor();
		const inputs = wrapper.findAll('input');
		const button = wrapper.get('.message-editor__body button').element as HTMLButtonElement;
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
		const button = wrapper.get('.message-editor__body button').element as HTMLButtonElement;

		expect(button.disabled).toBe(true);
		await inputs[1].setValue('登录用户留言');
		expect(button.disabled).toBe(false);
	});

	it('collapses the editor by default on mobile and allows toggling it', async () => {
		mediaQueryState.isMobile = true;
		const wrapper = mountEditor();
		const editor = wrapper.get('.message-editor');
		const openButton = wrapper
			.findAll('button')
			.find((button) => button.text().includes('写留言'));

		expect(openButton?.attributes('style') ?? '').not.toContain('display: none');
		expect(editor.attributes('style')).toContain('display: none');

		await openButton?.trigger('click');
		expect(wrapper.get('.message-editor').attributes('style') ?? '').not.toContain(
			'display: none',
		);

		const collapseButton = wrapper.get('.message-editor__collapse-button');
		expect(collapseButton.attributes('style') ?? '').not.toContain('display: none');
		await collapseButton.trigger('click');
		expect(wrapper.get('.message-editor').attributes('style')).toContain('display: none');
	});
});
