import { describe, expect, it } from 'vitest';

describe('frontend test environment', () => {
	it('provides a DOM environment', () => {
		const element = document.createElement('div');
		element.textContent = 'blog';

		expect(element.textContent).toBe('blog');
	});
});
