import confetti from 'canvas-confetti';

export function useLikeConfetti() {
	const triggerConfetti = (event: MouseEvent, angle: number = 90, spread: number = 90) => {
		const x = event.clientX / window.innerWidth;
		const y = event.clientY / window.innerHeight;

		confetti({
			particleCount: 80,
			angle,
			spread,
			origin: { x, y },
		});
	};

	return { triggerConfetti };
}
