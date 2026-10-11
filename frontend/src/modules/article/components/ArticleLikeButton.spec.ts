import { mount } from '@vue/test-utils';
import confetti from 'canvas-confetti';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ArticleLikeButton from './ArticleLikeButton.vue';

vi.mock('canvas-confetti', () => ({ default: vi.fn() }));

describe('ArticleLikeButton', () => {
	beforeEach(() => vi.clearAllMocks());

	it.each([false, true])(
		'plays the angled burst and emits toggle with isLiked=%s',
		async (isLiked) => {
			const wrapper = mount(ArticleLikeButton, {
				props: { expanded: false, hasCatalog: true, isLiked },
				global: { stubs: { ILucideThumbsUp: true } },
			});

			await wrapper.get('button').trigger('click', { clientX: 40, clientY: 300 });

			expect(wrapper.emitted('toggle')).toHaveLength(1);
			expect(confetti).toHaveBeenCalledExactlyOnceWith({
				particleCount: 80,
				angle: 45,
				spread: 90,
				origin: { x: 40 / window.innerWidth, y: 300 / window.innerHeight },
			});
			expect(wrapper.props('isLiked')).toBe(isLiked);
			wrapper.unmount();
		},
	);
});
