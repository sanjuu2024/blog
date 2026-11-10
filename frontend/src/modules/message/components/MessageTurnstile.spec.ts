import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Mock } from 'vitest';
import type { TurnstileApi, TurnstileRenderOptions } from '../types/turnstile';
import MessageTurnstile from './MessageTurnstile.vue';

const scriptState = vi.hoisted(() => ({ load: vi.fn() }));

vi.mock('@vueuse/core', () => ({
	useScriptTag: () => ({ load: scriptState.load }),
}));

vi.mock('@/composables/useTheme', async () => {
	const { ref } = await import('vue');
	return {
		useTheme: () => ({ resolvedTheme: ref('light') }),
	};
});

function mountTurnstile() {
	return mount(MessageTurnstile, {
		props: {
			siteKey: 'test-site-key',
			action: 'guest_message',
		},
		global: {
			stubs: {
				ElButton: {
					props: ['disabled'],
					template: '<button :disabled="disabled"><slot /></button>',
				},
				ILucideLoader: true,
				ILucideRefreshCw: true,
			},
		},
	});
}

describe('MessageTurnstile', () => {
	let render: Mock<TurnstileApi['render']>;
	let remove: Mock<TurnstileApi['remove']>;

	beforeEach(() => {
		vi.clearAllMocks();
		render = vi.fn<TurnstileApi['render']>(() => 'widget-id');
		remove = vi.fn<TurnstileApi['remove']>();
		window.turnstile = {
			render,
			remove,
			reset: vi.fn(),
		};
	});

	it('shows a loader until the widget is rendered', async () => {
		let resolveLoad: (() => void) | undefined;
		scriptState.load.mockImplementation(
			() => new Promise<void>((resolve) => (resolveLoad = resolve)),
		);
		const wrapper = mountTurnstile();

		expect(wrapper.find('[data-test="turnstile-loading"]').exists()).toBe(true);
		expect(render).not.toHaveBeenCalled();

		resolveLoad?.();
		await flushPromises();

		expect(wrapper.find('[data-test="turnstile-loading"]').exists()).toBe(false);
		expect(render).toHaveBeenCalledOnce();
		expect(
			wrapper.get('[data-test="turnstile-widget"]').attributes('style') ?? '',
		).not.toContain('display: none');
	});

	it('replaces a failed widget with an error state and supports retrying', async () => {
		scriptState.load.mockResolvedValue(undefined);
		const wrapper = mountTurnstile();
		await flushPromises();
		const options = render.mock.calls[0][1] as TurnstileRenderOptions;

		options['error-callback']?.();
		await flushPromises();

		expect(remove).toHaveBeenCalledWith('widget-id');
		expect(wrapper.get('[data-test="turnstile-error"]').text()).toContain('人机验证失败');
		const retryButton = wrapper.get('[data-test="turnstile-retry"]');
		expect((retryButton.element as HTMLButtonElement).disabled).toBe(false);

		await retryButton.trigger('click');
		await flushPromises();

		expect(render).toHaveBeenCalledTimes(2);
		expect(wrapper.find('[data-test="turnstile-error"]').exists()).toBe(false);
	});
});
