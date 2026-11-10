// 用户可选择的主题模式：system 表示跟随系统，light/dark 表示手动指定。
export const DEFAULT_THEME = 'system';

export const THEME_STORAGE_KEY = 'sanjuu-blog-theme';

export const themeNames = ['system', 'light', 'dark'] as const;

export type ThemeName = (typeof themeNames)[number];

// system 最终会解析成 light 或 dark，这个类型表示真正写入页面的主题。
export type ResolvedThemeName = Exclude<ThemeName, 'system'>;

export interface ThemeOption {
	name: ThemeName;
	label: string;
	swatch: string;
}

export const themeOptions: ThemeOption[] = [
	{
		name: 'system',
		label: '跟随系统',
		swatch: 'linear-gradient(135deg, #faf9f5 0 50%, #171717 50% 100%)',
	},
	{
		name: 'light',
		label: '浅色',
		swatch: '#faf9f5',
	},
	{
		name: 'dark',
		label: '深色',
		swatch: '#171717',
	},
];

export function isThemeName(value: string | null): value is ThemeName {
	return themeNames.includes(value as ThemeName);
}
