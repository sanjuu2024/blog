import { mount } from '@vue/test-utils';
import confetti from 'canvas-confetti';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ArticleLikeButton from './ArticleLikeButton.vue';
import AppFixedButton from '@/components/AppFixedButton.vue';

vi.mock('canvas-confetti', () => ({ default: vi.fn() }));

describe('ArticleLikeButton', () => {
	beforeEach(() => vi.clearAllMocks());

	it.each([false, true])(
		'emits toggle and only plays the angled burst for a new like with isLiked=%s',
		async (isLiked) => {
			const wrapper = mount(ArticleLikeButton, {
				props: { expanded: false, hasCatalog: true, isLiked },
				global: { stubs: { ILucideThumbsUp: true } },
			});

			await wrapper.get('button').trigger('click', { clientX: 40, clientY: 300 });

			expect(wrapper.emitted('toggle')).toHaveLength(1);
			if (isLiked) {
				expect(confetti).not.toHaveBeenCalled();
			} else {
				expect(confetti).toHaveBeenCalledExactlyOnceWith({
					particleCount: 80,
					angle: 45,
					spread: 90,
					origin: { x: 40 / window.innerWidth, y: 300 / window.innerHeight },
				});
			}
			expect(wrapper.props('isLiked')).toBe(isLiked);
			wrapper.unmount();
		},
	);

	it('keeps the catalog-dependent position and expanded state on the shared button', async () => {
		const wrapper = mount(ArticleLikeButton, {
			props: { expanded: false, hasCatalog: true, isLiked: false },
			global: { stubs: { ILucideThumbsUp: true } },
		});
		const fixedButton = wrapper.getComponent(AppFixedButton);

		expect(fixedButton.props()).toMatchObject({ placement: 'bottom-left', offsetY: 100 });
		await wrapper.setProps({ hasCatalog: false });
		expect(fixedButton.props('offsetY')).toBe(32);
		await wrapper.setProps({ hasCatalog: true, expanded: true, isLiked: true });
		expect(fixedButton.props('offsetY')).toBe(100);
		expect(fixedButton.classes()).toContain('article-like-button-expanded');
		expect(wrapper.find('.article-like-button--filled').exists()).toBe(true);
		wrapper.unmount();
	});
});
