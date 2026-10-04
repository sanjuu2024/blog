export interface TurnstileRenderOptions {
	sitekey: string;
	action?: string;
	theme?: 'light' | 'dark' | 'auto';
	size?: 'normal' | 'compact' | 'flexible';
	callback: (token: string) => void;
	'expired-callback'?: () => void;
	'error-callback'?: () => void;
}

export interface TurnstileApi {
	render: (container: HTMLElement, options: TurnstileRenderOptions) => string | number;
	reset: (widgetId?: string | number) => void;
	remove: (widgetId?: string | number) => void;
}

declare global {
	interface Window {
		turnstile?: TurnstileApi;
	}
}
