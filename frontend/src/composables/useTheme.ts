import { computed, readonly, ref } from 'vue';
import {
	DEFAULT_THEME,
	THEME_STORAGE_KEY,
	isThemeName,
	themeOptions,
	type ResolvedThemeName,
	type ThemeName,
} from '@/constants/theme';

// 用户选择的主题模式，可能是 system / light / dark。
const currentTheme = ref<ThemeName>(DEFAULT_THEME);

// 页面真正生效的主题，只会是 light 或 dark。
const resolvedTheme = ref<ResolvedThemeName>('light');

// 模块级初始化开关：防止多个组件调用 useTheme() 时重复初始化。
let initialized = false;

// 缓存系统主题查询对象，避免重复注册 prefers-color-scheme 监听器。
let systemThemeQuery: MediaQueryList | null = null;

function readStoredTheme(): ThemeName {
	if (typeof window === 'undefined') {
		return DEFAULT_THEME;
	}

	// localStorage 里的值不一定可信，读取后要先校验。
	const storedTheme = window.localStorage.getItem(THEME_STORAGE_KEY);
	return isThemeName(storedTheme) ? storedTheme : DEFAULT_THEME;
}

function getSystemTheme(): ResolvedThemeName {
	if (typeof window === 'undefined' || !window.matchMedia) {
		return 'light';
	}

	return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

// system 是用户选择的模式，不是 CSS 主题；真正生效时要先解析成 light 或 dark。
function resolveTheme(theme: ThemeName): ResolvedThemeName {
	return theme === 'system' ? getSystemTheme() : theme;
}

function applyTheme(theme: ThemeName) {
	if (typeof document === 'undefined') {
		return;
	}

	const nextTheme = resolveTheme(theme);
	resolvedTheme.value = nextTheme;

	// data-theme 给 CSS 选择器使用，data-theme-mode 保留用户的原始选择。
	document.documentElement.dataset.theme = nextTheme;
	document.documentElement.dataset.themeMode = theme;
	document.documentElement.style.colorScheme = nextTheme;
}

function handleSystemThemeChange() {
	// 只有在“跟随系统”模式下，系统主题变化才需要影响页面。
	if (currentTheme.value === 'system') {
		applyTheme(currentTheme.value);
	}
}

function watchSystemTheme() {
	if (typeof window === 'undefined' || !window.matchMedia || systemThemeQuery) {
		return;
	}

	systemThemeQuery = window.matchMedia('(prefers-color-scheme: dark)');
	systemThemeQuery.addEventListener('change', handleSystemThemeChange);
}

export function setupTheme() {
	if (initialized) {
		return;
	}

	// setupTheme 可能被 main.ts 和组件内 useTheme 同时触发，只允许第一次真正执行。
	initialized = true;
	currentTheme.value = readStoredTheme();
	applyTheme(currentTheme.value);
	watchSystemTheme();
}

export function setTheme(theme: ThemeName) {
	currentTheme.value = theme;
	applyTheme(theme);

	// 保存用户选择；如果是 system，也保存 system，而不是保存解析后的 light/dark。
	if (typeof window !== 'undefined') {
		window.localStorage.setItem(THEME_STORAGE_KEY, theme);
	}
}

export function useTheme() {
	// 允许组件直接使用 useTheme，即使 main.ts 忘记调用 setupTheme 也能正常工作。
	if (!initialized) {
		setupTheme();
	}

	return {
		currentTheme: readonly(currentTheme),
		resolvedTheme: readonly(resolvedTheme),
		isDarkTheme: computed(() => resolvedTheme.value === 'dark'),
		setTheme,
		themeOptions,
	};
}
