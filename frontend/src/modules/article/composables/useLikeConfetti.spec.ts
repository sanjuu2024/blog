import confetti from 'canvas-confetti';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useLikeConfetti } from './useLikeConfetti';

vi.mock('canvas-confetti', () => ({ default: vi.fn() }));

describe('useLikeConfetti', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.stubGlobal('innerWidth', 1000);
		vi.stubGlobal('innerHeight', 800);
	});

	afterEach(() => vi.unstubAllGlobals());

	it('uses viewport coordinates and the default burst options', () => {
		const { triggerConfetti } = useLikeConfetti();
		triggerConfetti(new MouseEvent('click', { clientX: 250, clientY: 400 }));

		expect(confetti).toHaveBeenCalledExactlyOnceWith({
			particleCount: 80,
			angle: 90,
			spread: 90,
			origin: { x: 0.25, y: 0.5 },
		});
	});

	it('accepts a custom angle and spread without changing the click origin', () => {
		const { triggerConfetti } = useLikeConfetti();
		triggerConfetti(new MouseEvent('click', { clientX: 100, clientY: 600 }), 45, 60);

		expect(confetti).toHaveBeenCalledExactlyOnceWith({
			particleCount: 80,
			angle: 45,
			spread: 60,
			origin: { x: 0.1, y: 0.75 },
		});
	});
});
