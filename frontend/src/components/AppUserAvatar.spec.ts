import { mount } from '@vue/test-utils';
import { ElAvatar } from 'element-plus';
import { describe, expect, it } from 'vitest';
import AppUserAvatar from './AppUserAvatar.vue';

describe('AppUserAvatar', () => {
	const avatarGlobal = {
		components: { ElAvatar },
		stubs: {
			ILetsIconsLightningFill: true,
			ISolarUserBold: true,
		},
	};

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

	it('restores the letter avatar background when the image fails to load', async () => {
		const props = { name: 'Alice', userId: 10001, isAdmin: true };
		const fallback = mount(AppUserAvatar, { props, global: avatarGlobal });
		const fallbackBackground = (fallback.get('.user-avatar').element as HTMLElement).style
			.backgroundColor;
		const wrapper = mount(AppUserAvatar, {
			props: { ...props, avatarUrl: 'https://img.example.com/broken.png' },
			global: avatarGlobal,
		});

		expect((wrapper.get('.user-avatar').element as HTMLElement).style.backgroundColor).toBe(
			'var(--app-bg)',
		);
		await wrapper.get('img').trigger('error');

		expect(wrapper.find('img').exists()).toBe(false);
		expect(wrapper.get('.user-avatar').text()).toBe('A');
		expect((wrapper.get('.user-avatar').element as HTMLElement).style.backgroundColor).toBe(
			fallbackBackground,
		);
		expect(fallbackBackground).not.toBe('var(--app-bg)');
		expect(wrapper.find('.user-avatar-badge').exists()).toBe(true);
		wrapper.unmount();
		fallback.unmount();
	});

	it('resets the image failure when the URL changes and handles clearing the URL', async () => {
		const wrapper = mount(AppUserAvatar, {
			props: {
				name: 'Alice',
				userId: 10001,
				avatarUrl: 'https://img.example.com/broken.png',
			},
			global: avatarGlobal,
		});
		await wrapper.get('img').trigger('error');
		const fallbackBackground = (wrapper.get('.user-avatar').element as HTMLElement).style
			.backgroundColor;

		await wrapper.setProps({ avatarUrl: 'https://img.example.com/new.png' });

		expect(wrapper.get('img').attributes('src')).toBe('https://img.example.com/new.png');
		expect((wrapper.get('.user-avatar').element as HTMLElement).style.backgroundColor).toBe(
			'var(--app-bg)',
		);
		await wrapper.setProps({ avatarUrl: '' });

		expect(wrapper.find('img').exists()).toBe(false);
		expect(wrapper.get('.user-avatar').text()).toBe('A');
		expect((wrapper.get('.user-avatar').element as HTMLElement).style.backgroundColor).toBe(
			fallbackBackground,
		);
		wrapper.unmount();
	});

	it.each(['guest', 'deleted'] as const)(
		'keeps the default icon background for a %s avatar after an image failure',
		async (kind) => {
			const wrapper = mount(AppUserAvatar, {
				props: {
					name: 'Alice',
					avatarUrl: 'https://img.example.com/broken.png',
					isAdmin: true,
					[kind]: true,
				},
				global: avatarGlobal,
			});

			await wrapper.get('img').trigger('error');

			expect(wrapper.find('img').exists()).toBe(false);
			expect(wrapper.get('.user-avatar').find('svg').exists()).toBe(true);
			expect(wrapper.get('.user-avatar').text()).toBe('');
			expect((wrapper.get('.user-avatar').element as HTMLElement).style.backgroundColor).toBe(
				'var(--app-bg)',
			);
			expect(wrapper.find('.user-avatar-badge').exists()).toBe(false);
			wrapper.unmount();
		},
	);
});
