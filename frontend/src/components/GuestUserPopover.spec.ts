import { mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { describe, expect, it } from 'vitest';
import GuestUserPopover from './GuestUserPopover.vue';

const ElPopoverStub = defineComponent({
	template: `
		<div>
			<div class="popover-reference"><slot name="reference" /></div>
			<div class="popover-content"><slot /></div>
		</div>
	`,
});

const AppUserAvatarStub = defineComponent({
	props: {
		name: String,
		size: Number,
	},
	template: '<span class="guest-avatar" />',
});

describe('GuestUserPopover', () => {
	it('uses the default slot as reference and shows the guest identity', () => {
		const wrapper = mount(GuestUserPopover, {
			props: { nickname: '访客小明' },
			slots: {
				default: '<span class="guest-trigger">游客头像</span>',
			},
			global: {
				stubs: {
					ElPopover: ElPopoverStub,
					AppUserAvatar: AppUserAvatarStub,
				},
			},
		});

		expect(wrapper.get('.guest-trigger').text()).toBe('游客头像');
		expect(wrapper.get('.guest-user-popover__identity').text()).toContain('访客小明');
		expect(wrapper.get('.guest-user-popover__identity').text()).toContain('游客');
		expect(wrapper.getComponent(AppUserAvatarStub).props()).toMatchObject({
			name: '访客小明',
			size: 48,
		});
	});
});
