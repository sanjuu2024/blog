import { flushPromises, mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppFooter from './AppFooter.vue';

describe('AppFooter', () => {
	beforeEach(() => {
		vi.useFakeTimers();
		vi.setSystemTime(new Date('2026-10-30T01:02:03+08:00'));
		vi.stubEnv('VITE_SITE_LAUNCHED_AT', '2026-10-29T00:00:00+08:00');
	});

	afterEach(() => {
		vi.useRealTimers();
		vi.unstubAllEnvs();
	});

	it('shows copyright, the RSS link, and a live uptime counter', async () => {
		const wrapper = mount(AppFooter);
		await flushPromises();

		expect(wrapper.get('.app-footer__copyright').text()).toBe(
			`Copyright © 2026-2026 ${import.meta.env.VITE_APP_TITLE || 'Sanjuu Blog'}. All Rights Reserved.`,
		);
		expect(wrapper.get('.app-footer__rss').attributes('href')).toBe('/rss.xml');
		expect(wrapper.get('.app-footer__rss').attributes('type')).toBe('application/rss+xml');
		expect(wrapper.get('.app-footer__uptime').text()).toBe(
			'🎉 本站已运行 1 天 01 小时 02 分 03 秒 ♪(´▽｀)',
		);

		await vi.advanceTimersByTimeAsync(1000);
		expect(wrapper.get('.app-footer__uptime').text()).toBe(
			'🎉 本站已运行 1 天 01 小时 02 分 04 秒 ♪(´▽｀)',
		);
		wrapper.unmount();
	});

	it('hides the uptime when the launch time is invalid', async () => {
		vi.stubEnv('VITE_SITE_LAUNCHED_AT', 'invalid');
		const wrapper = mount(AppFooter);
		await flushPromises();

		expect(wrapper.find('.app-footer__uptime').exists()).toBe(false);
		wrapper.unmount();
	});
});
