import 'axios';

declare module 'axios' {
	interface AxiosRequestConfig {
		meta?: {
			showError?: boolean;
			skipAuthRefresh?: boolean;
		};
		_retry?: boolean;
	}
}
