import { beforeEach, describe, expect, it, vi } from 'vitest';

describe('useTheme', () => {
	let systemDark = false;
	let systemThemeListener: (() => void) | undefined;

	beforeEach(() => {
		vi.resetModules();
		window.localStorage.clear();
		document.documentElement.removeAttribute('data-theme');
		document.documentElement.removeAttribute('data-theme-mode');
		document.documentElement.style.colorScheme = '';
		systemDark = false;
		systemThemeListener = undefined;

		Object.defineProperty(window, 'matchMedia', {
			configurable: true,
			value: vi.fn(() => ({
				get matches() {
					return systemDark;
				},
				media: '(prefers-color-scheme: dark)',
				onchange: null,
				addEventListener: vi.fn((_event: string, listener: () => void) => {
					systemThemeListener = listener;
				}),
				removeEventListener: vi.fn(),
				addListener: vi.fn(),
				removeListener: vi.fn(),
				dispatchEvent: vi.fn(),
			})),
		});
	});

	it('falls back to system mode when the stored value is invalid', async () => {
		window.localStorage.setItem('sanjuu-blog-theme', 'invalid-theme');
		systemDark = true;
		const { useTheme } = await import('./useTheme');

		const theme = useTheme();

		expect(theme.currentTheme.value).toBe('system');
		expect(theme.resolvedTheme.value).toBe('dark');
		expect(theme.isDarkTheme.value).toBe(true);
		expect(document.documentElement.dataset.theme).toBe('dark');
		expect(document.documentElement.dataset.themeMode).toBe('system');
		expect(document.documentElement.style.colorScheme).toBe('dark');
	});

	it('persists and applies a manually selected theme', async () => {
		const { useTheme } = await import('./useTheme');
		const theme = useTheme();

		theme.setTheme('dark');

		expect(theme.currentTheme.value).toBe('dark');
		expect(theme.resolvedTheme.value).toBe('dark');
		expect(window.localStorage.getItem('sanjuu-blog-theme')).toBe('dark');
		expect(document.documentElement.dataset.theme).toBe('dark');
	});

	it('updates the resolved theme when the system preference changes', async () => {
		const { useTheme } = await import('./useTheme');
		const theme = useTheme();
		expect(theme.resolvedTheme.value).toBe('light');

		systemDark = true;
		systemThemeListener?.();

		expect(theme.currentTheme.value).toBe('system');
		expect(theme.resolvedTheme.value).toBe('dark');
	});

	it('ignores system preference changes in manual mode', async () => {
		const { useTheme } = await import('./useTheme');
		const theme = useTheme();
		theme.setTheme('light');

		systemDark = true;
		systemThemeListener?.();

		expect(theme.currentTheme.value).toBe('light');
		expect(theme.resolvedTheme.value).toBe('light');
	});
});
