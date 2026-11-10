import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent, nextTick, watch } from 'vue';
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
				ElDialog: defineComponent({
					props: ['modelValue'],
					emits: ['opened', 'closed', 'update:modelValue'],
					setup(props, { emit }) {
						watch(
							() => props.modelValue,
							async (visible) => {
								await nextTick();
								emit(visible ? 'opened' : 'closed');
							},
							{ flush: 'post' },
						);
					},
					template:
						'<div v-if="modelValue" data-test="turnstile-dialog"><button data-test="dialog-close" @click="$emit(\'update:modelValue\', false)">关闭</button><slot /></div>',
				}),
				MessageTurnstile: defineComponent({
					emits: ['verified'],
					template:
						'<button data-test="turnstile" @click="$emit(\'verified\', \'test-token\')">验证</button>',
				}),
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
					props: ['disabled', 'nativeType'],
					template:
						'<button :disabled="disabled" :type="nativeType || \'button\'"><slot /></button>',
				},
				ILucideLoader: true,
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
		expect(wrapper.find('[data-test="turnstile"]').exists()).toBe(false);

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

	it('shows Turnstile on demand and submits automatically after guest verification', async () => {
		const guestWrapper = mountEditor();
		const guestInputs = guestWrapper.findAll('input');
		await guestInputs[0].setValue('访客');
		await guestInputs[3].setValue('留言内容');

		expect(guestWrapper.find('[data-test="turnstile"]').exists()).toBe(false);
		await guestWrapper.get('form').trigger('submit');
		await flushPromises();
		expect(guestWrapper.emitted('submit')).toBeUndefined();
		expect(guestWrapper.find('[data-test="turnstile-dialog"]').exists()).toBe(true);
		expect(guestWrapper.get('.message-editor__body button').text()).toContain('验证中');
		expect(guestWrapper.find('[data-test="submit-verifying-icon"]').exists()).toBe(true);
		expect(
			(guestWrapper.get('.message-editor__body button').element as HTMLButtonElement)
				.disabled,
		).toBe(true);

		const turnstileButton = guestWrapper.get('[data-test="turnstile"]');
		expect((turnstileButton.element as HTMLButtonElement).disabled).toBe(false);
		await turnstileButton.trigger('click');

		expect(guestWrapper.emitted('submit')?.[0]).toEqual([
			{
				nickname: '访客',
				email: undefined,
				content: '留言内容',
				notifyOnReply: false,
				turnstileToken: 'test-token',
			},
		]);

		const memberWrapper = mountEditor(true);
		await memberWrapper.findAll('input')[1].setValue('登录用户留言');
		await memberWrapper.get('form').trigger('submit');
		await flushPromises();

		expect(memberWrapper.emitted('submit')?.[0]?.[0]).not.toHaveProperty('turnstileToken');
		expect(memberWrapper.find('[data-test="turnstile"]').exists()).toBe(false);
	});

	it('keeps guest input when resetting Turnstile after a failed request', async () => {
		const wrapper = mountEditor();
		const inputs = wrapper.findAll('input');
		await inputs[0].setValue('访客');
		await inputs[3].setValue('留言内容');
		await wrapper.get('form').trigger('submit');
		await flushPromises();

		expect(wrapper.find('[data-test="turnstile"]').exists()).toBe(true);
		(wrapper.vm as unknown as { resetTurnstile: () => void }).resetTurnstile();
		await wrapper.vm.$nextTick();

		expect(wrapper.find('[data-test="turnstile"]').exists()).toBe(false);
		expect((inputs[0].element as HTMLInputElement).value).toBe('访客');
		expect((inputs[3].element as HTMLInputElement).value).toBe('留言内容');
	});

	it('keeps guest input when the verification dialog is cancelled', async () => {
		const wrapper = mountEditor();
		const inputs = wrapper.findAll('input');
		await inputs[0].setValue('访客');
		await inputs[3].setValue('留言内容');
		await wrapper.get('form').trigger('submit');
		await flushPromises();

		await wrapper.get('[data-test="dialog-close"]').trigger('click');
		await flushPromises();

		expect(wrapper.find('[data-test="turnstile-dialog"]').exists()).toBe(false);
		expect((inputs[0].element as HTMLInputElement).value).toBe('访客');
		expect((inputs[3].element as HTMLInputElement).value).toBe('留言内容');
		expect(
			(wrapper.get('.message-editor__body button').element as HTMLButtonElement).disabled,
		).toBe(false);
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
