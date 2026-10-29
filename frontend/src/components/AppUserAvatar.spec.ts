import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import AppUserAvatar from './AppUserAvatar.vue';

describe('AppUserAvatar', () => {
	it('uses the default badge size and allows overriding it with a percentage', async () => {
		const wrapper = mount(AppUserAvatar, {
			props: { isAdmin: true },
			global: {
				stubs: {
					ElAvatar: true,
					ILetsIconsLightningFill: true,
					ISolarUserBold: true,
				},
			},
		});
		const badge = wrapper.get('.user-avatar-badge');

		expect(badge.attributes('style')).toContain('width: 45%');
		expect(badge.attributes('style')).toContain('height: 45%');

		await wrapper.setProps({ badgeSizePercent: 30 });

		expect(badge.attributes('style')).toContain('width: 30%');
		expect(badge.attributes('style')).toContain('height: 30%');
		wrapper.unmount();
	});
});
