import type { LocationQueryValue, Router } from 'vue-router';

// 登录与注册共用回跳目标校验，保留站内 query/hash，避免外部跳转与认证页循环。
export function getAuthRedirect(
	router: Router,
	value: LocationQueryValue | LocationQueryValue[] | undefined,
) {
	const target = Array.isArray(value) ? value[0] : value;
	if (
		typeof target !== 'string' ||
		!/^\/(?!\/)/.test(target) ||
		target.includes('\\') ||
		Array.from(target).some((char) => char.charCodeAt(0) <= 32)
	) {
		return undefined;
	}
	const route = router.resolve(target);
	if (
		!route.matched.length ||
		route.matched.some(({ name }) => name === 'Any' || name === 'AuthLayout')
	) {
		return undefined;
	}
	return route.fullPath;
}
