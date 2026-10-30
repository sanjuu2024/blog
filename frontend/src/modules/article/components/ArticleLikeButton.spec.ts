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
				props: { expanded: false, offsetY: 168, isLiked, likeCount: 0 },
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

	it('uses the supplied position and keeps the expanded and liked states', async () => {
		const wrapper = mount(ArticleLikeButton, {
			props: { expanded: false, offsetY: 168, isLiked: false, likeCount: 0 },
			global: { stubs: { ILucideThumbsUp: true } },
		});
		const fixedButton = wrapper.getComponent(AppFixedButton);

		expect(fixedButton.props()).toMatchObject({ placement: 'bottom-left', offsetY: 168 });
		expect(fixedButton.get('button').element.style.height).toBe('66px');
		expect(wrapper.get('.article-like-button__count').text()).toBe('0');
		await wrapper.setProps({ offsetY: 100, likeCount: 12 });
		expect(fixedButton.props('offsetY')).toBe(100);
		expect(wrapper.get('.article-like-button__count').text()).toBe('12');
		await wrapper.setProps({ offsetY: 168, expanded: true, isLiked: true });
		expect(fixedButton.props('offsetY')).toBe(168);
		expect(fixedButton.classes()).toContain('article-like-button-expanded');
		expect(wrapper.find('.article-like-button--filled').exists()).toBe(true);
		wrapper.unmount();
	});
});
