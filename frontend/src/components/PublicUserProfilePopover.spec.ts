import { mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getPublicUserProfile } from '@/modules/user/api/userApi';
import PublicUserProfilePopover from './PublicUserProfilePopover.vue';

vi.mock('@/modules/user/api/userApi', () => ({
	getPublicUserProfile: vi.fn(),
}));

const ElPopoverStub = defineComponent({
	emits: ['before-enter'],
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

	it('uses the default slot as reference and loads the profile only once', async () => {
		vi.mocked(getPublicUserProfile).mockResolvedValue({
			id: 10001,
			username: 'admin',
			nickname: '管理员',
			avatarUrl: '',
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
				bio: '重试成功',
			});
		const wrapper = mountPopover();

		await wrapper.get('.popover-reference').trigger('mouseenter');
		await vi.waitFor(() => expect(wrapper.text()).toContain('公开资料加载失败'));
		await wrapper.get('button').trigger('click');
		await vi.waitFor(() => expect(wrapper.text()).toContain('重试成功'));

		expect(getPublicUserProfile).toHaveBeenCalledTimes(2);
	});
});
