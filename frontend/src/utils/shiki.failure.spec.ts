import { afterEach, describe, expect, it, vi } from 'vitest';
import { highlightCodeUnder } from './shiki';

vi.mock('shiki/core', () => {
	throw new Error('Highlight engine failed to load');
});

afterEach(() => document.body.replaceChildren());

describe('Shiki loading failure', () => {
	it('keeps the original code and toolbar working when the engine cannot load', async () => {
		const container = document.createElement('div');
		container.innerHTML = '<pre><code class="language-js">const n = 1;\n</code></pre>';
		document.body.append(container);
		const controller = new AbortController();
		const copy = vi.fn().mockResolvedValue(undefined);
		try {
			await highlightCodeUnder(container, { signal: controller.signal, copy });
			expect(container.querySelector('code')?.textContent).toBe('const n = 1;\n');
			expect(container.querySelector('.line')).not.toBeNull();
			container.querySelector<HTMLButtonElement>('.copy-to-clipboard-button')!.click();
			await vi.waitFor(() => expect(copy).toHaveBeenCalledWith('const n = 1;\n'));
			container.querySelector<HTMLButtonElement>('.app-code-collapse-button')!.click();
			expect(container.querySelector('.is-code-collapsed')).not.toBeNull();
		} finally {
			controller.abort();
		}
	});
});
