import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import type { PublicFriendLinkItem } from '../types/friendLink';
import FriendLinkItem from './FriendLinkItem.vue';

const friendLink: PublicFriendLinkItem = {
	id: 110001,
	name: 'Example Blog',
	url: 'https://example.com',
	logoUrl: 'https://example.com/logo.png',
	description: 'A sample description',
	section: 'FRIENDS',
	sortNo: 100,
};

describe('FriendLinkItem', () => {
	it('uses AppImage for the optional representative image and opens the external site safely', () => {
		const wrapper = mount(FriendLinkItem, {
			props: { friendLink },
			global: {
				stubs: {
					AppImage: {
						props: ['url', 'alt', 'rounded'],
						template:
							'<div class="app-image" :data-url="url" :data-alt="alt" :data-rounded="rounded" />',
					},
				},
			},
		});

		const link = wrapper.get('a');
		const image = wrapper.get('.app-image');
		expect(link.attributes('href')).toBe(friendLink.url);
		expect(link.attributes('target')).toBe('_blank');
		expect(link.attributes('rel')).toBe('noopener noreferrer');
		expect(image.attributes('data-url')).toBe(friendLink.logoUrl);
		expect(image.attributes('data-alt')).toBe(friendLink.name);
		expect(image.attributes('data-rounded')).toBe('false');
		expect(wrapper.text()).toContain(friendLink.name);
		expect(wrapper.text()).toContain(friendLink.description);
		expect(wrapper.find('.friend-link-item__content').exists()).toBe(true);
	});

	it('renders the icon fallback when the representative image URL is empty', () => {
		const wrapper = mount(FriendLinkItem, {
			props: { friendLink: { ...friendLink, name: 'example Blog', logoUrl: '' } },
			global: {
				stubs: {
					AppImage: {
						props: ['url'],
						template: '<div class="app-image" :data-url="url" />',
					},
				},
			},
		});

		expect(wrapper.find('.app-image').exists()).toBe(false);
		expect(wrapper.find('.friend-link-item__logo-fallback').exists()).toBe(true);
		expect(wrapper.find('.friend-link-item__logo-fallback svg').exists()).toBe(true);
	});
});
