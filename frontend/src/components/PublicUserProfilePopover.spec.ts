import { mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { getPublicUserProfile } from '@/modules/user/api/userApi';
import PublicUserProfilePopover from './PublicUserProfilePopover.vue';

vi.mock('@/modules/user/api/userApi', () => ({
	getPublicUserProfile: vi.fn(),
}));

const ElPopoverStub = defineComponent({
	emits: ['before-enter', 'after-enter', 'hide'],
	template: `
		<div>
			<div class="popover-reference" @mouseenter="$emit('before-enter')">
				<slot name="reference" />
			</div>
			<div class="popover-content"><slot /></div>
		</div>
	`,
});

const ElButtonStub = defineComponent({
	emits: ['click'],
	template: '<button @click="$emit(\'click\')"><slot /></button>',
});

function mountPopover() {
	return mount(PublicUserProfilePopover, {
		props: { userId: 10001 },
		slots: {
			default: '<span class="author-trigger">文章作者</span>',
		},
		global: {
			stubs: {
				ElPopover: ElPopoverStub,
				ElButton: ElButtonStub,
				AppUserAvatar: true,
				ILucideLoader: true,
			},
		},
	});
}

describe('PublicUserProfilePopover', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});
	afterEach(() => vi.restoreAllMocks());

	it('uses the default slot as reference and loads the profile only once', async () => {
		vi.mocked(getPublicUserProfile).mockResolvedValue({
			id: 10001,
			username: 'admin',
			nickname: '管理员',
			avatarUrl: '',
			role: 'ADMIN',
			bio: '站点作者',
		});
		const wrapper = mountPopover();

		expect(wrapper.get('.author-trigger').text()).toBe('文章作者');
		expect(getPublicUserProfile).not.toHaveBeenCalled();

		await wrapper.get('.popover-reference').trigger('mouseenter');
		await vi.waitFor(() => expect(wrapper.text()).toContain('站点作者'));
		expect(wrapper.text()).toContain('管理员');
		expect(wrapper.text()).toContain('@admin');
		expect(getPublicUserProfile).toHaveBeenCalledWith(10001, {
			meta: { showError: false },
		});

		await wrapper.get('.popover-reference').trigger('mouseenter');
		expect(getPublicUserProfile).toHaveBeenCalledTimes(1);
	});

	it('shows the default text when the profile bio is blank', async () => {
		vi.mocked(getPublicUserProfile).mockResolvedValue({
			id: 10001,
			username: 'admin',
			nickname: '管理员',
			avatarUrl: '',
			role: 'ADMIN',
			bio: '  ',
		});
		const wrapper = mountPopover();

		await wrapper.get('.popover-reference').trigger('mouseenter');
		await vi.waitFor(() => {
			expect(wrapper.text()).toContain('这个家伙很懒，什么也没有留下');
		});
	});

	it('allows retrying after the profile request fails', async () => {
		vi.mocked(getPublicUserProfile)
			.mockRejectedValueOnce(new Error('network error'))
			.mockResolvedValueOnce({
				id: 10001,
				username: 'admin',
				nickname: '管理员',
				avatarUrl: '',
				role: 'ADMIN',
				bio: '重试成功',
			});
		const wrapper = mountPopover();

		await wrapper.get('.popover-reference').trigger('mouseenter');
		await vi.waitFor(() => expect(wrapper.text()).toContain('公开资料加载失败'));
		await wrapper.get('button').trigger('click');
		await vi.waitFor(() => expect(wrapper.text()).toContain('重试成功'));

		expect(getPublicUserProfile).toHaveBeenCalledTimes(2);
	});

	it('offers expansion only for overflowing biographies and resets it when the popover closes', async () => {
		vi.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockReturnValue(240);
		vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(72);
		vi.mocked(getPublicUserProfile).mockResolvedValue({
			id: 10001,
			username: 'admin',
			nickname: '管理员',
			avatarUrl: '',
			role: 'ADMIN',
			bio: '一行简介\n\n\n\n\n\n最后一行',
		});
		const wrapper = mountPopover();
		await wrapper.get('.popover-reference').trigger('mouseenter');
		await vi.waitFor(() =>
			expect(wrapper.find('.public-user-profile-popover__bio-toggle').exists()).toBe(true),
		);

		const button = wrapper.get('.public-user-profile-popover__bio-toggle');
		expect(button.text()).toBe('展开');
		expect(button.attributes('aria-expanded')).toBe('false');
		await button.trigger('click');
		expect(wrapper.get('.public-user-profile-popover__bio').classes()).toContain(
			'public-user-profile-popover__bio--expanded',
		);
		expect(button.text()).toBe('收起');
		expect(button.attributes('aria-expanded')).toBe('true');
		await button.trigger('click');
		expect(wrapper.get('.public-user-profile-popover__bio').classes()).not.toContain(
			'public-user-profile-popover__bio--expanded',
		);
		await button.trigger('click');
		wrapper.getComponent(ElPopoverStub).vm.$emit('hide');
		await wrapper.vm.$nextTick();
		expect(button.text()).toBe('展开');
		expect(wrapper.get('.public-user-profile-popover__bio').classes()).not.toContain(
			'public-user-profile-popover__bio--expanded',
		);
		wrapper.unmount();
	});

	it('does not show an expansion control for a short biography or one-pixel rounding difference', async () => {
		vi.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockReturnValue(73);
		vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(72);
		vi.mocked(getPublicUserProfile).mockResolvedValue({
			id: 10001,
			username: 'admin',
			nickname: '管理员',
			avatarUrl: '',
			role: 'ADMIN',
			bio: '短简介',
		});
		const wrapper = mountPopover();
		await wrapper.get('.popover-reference').trigger('mouseenter');
		await vi.waitFor(() => expect(wrapper.text()).toContain('短简介'));
		expect(wrapper.find('.public-user-profile-popover__bio-toggle').exists()).toBe(false);
		wrapper.unmount();
	});

	it('remeasures overflow after the popover becomes visible and resets on a different user', async () => {
		let overflowing = false;
		vi.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockImplementation(() =>
			overflowing ? 240 : 0,
		);
		vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockImplementation(() =>
			overflowing ? 72 : 0,
		);
		vi.mocked(getPublicUserProfile)
			.mockResolvedValueOnce({
				id: 10001,
				username: 'admin',
				nickname: '管理员',
				avatarUrl: '',
				role: 'ADMIN',
				bio: '长简介',
			})
			.mockResolvedValueOnce({
				id: 10002,
				username: 'test',
				nickname: '测试用户',
				avatarUrl: '',
				role: 'USER',
				bio: '新简介',
			});
		const wrapper = mountPopover();
		await wrapper.get('.popover-reference').trigger('mouseenter');
		await vi.waitFor(() => expect(wrapper.text()).toContain('长简介'));
		expect(wrapper.find('.public-user-profile-popover__bio-toggle').exists()).toBe(false);
		overflowing = true;
		wrapper.getComponent(ElPopoverStub).vm.$emit('after-enter');
		await wrapper.vm.$nextTick();
		await wrapper.get('.public-user-profile-popover__bio-toggle').trigger('click');
		overflowing = false;
		await wrapper.setProps({ userId: 10002 });
		await wrapper.get('.popover-reference').trigger('mouseenter');
		await vi.waitFor(() => expect(wrapper.text()).toContain('新简介'));
		expect(wrapper.find('.public-user-profile-popover__bio-toggle').exists()).toBe(false);
		expect(wrapper.get('.public-user-profile-popover__bio').classes()).not.toContain(
			'public-user-profile-popover__bio--expanded',
		);
		wrapper.unmount();
	});
});
