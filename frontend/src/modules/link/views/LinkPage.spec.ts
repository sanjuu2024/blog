import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { usePublicFriendLinks } from '../composables/usePublicFriendLinks';
import type { PublicFriendLinkSection } from '../types/friendLink';
import LinkPage from './LinkPage.vue';

vi.mock('../composables/usePublicFriendLinks', () => ({
	usePublicFriendLinks: vi.fn(),
}));

function makeSection(
	section: PublicFriendLinkSection['section'],
	name: string,
	records: PublicFriendLinkSection['records'] = [],
): PublicFriendLinkSection {
	return { section, name, records };
}

describe('LinkPage', () => {
	beforeEach(() => vi.clearAllMocks());

	it('renders the fixed groups, external links, empty states, and application guidance', async () => {
		const loadFriendLinks = vi.fn();
		vi.mocked(usePublicFriendLinks).mockReturnValue({
			siteProfile: ref({
				name: '青禾边',
				url: 'https://blog.example.com',
				description: '聆听心中最初的声音',
				logoUrl: 'https://img.example.com/site-logo.png',
			}),
			sections: ref([
				makeSection('FRIENDS', '友人帐', [
					{
						id: 1,
						name: 'Example',
						url: 'https://example.com',
						logoUrl: 'https://example.com/logo.png',
						description: 'Example site',
						section: 'FRIENDS',
						sortNo: 100,
					},
				]),
				makeSection('EXPERTS', '大佬们'),
				makeSection('PEERS', '同行者'),
			]),
			loading: ref(false),
			loadFailed: ref(false),
			loadFriendLinks,
		});

		const wrapper = mount(LinkPage, {
			global: {
				stubs: {
					AppLoading: {
						props: ['loading'],
						template: '<div><slot v-if="!loading" /></div>',
					},
					ElButton: { template: '<button><slot /></button>' },
					AppImage: true,
					RouterLink: {
						props: ['to'],
						template: '<a :href="to"><slot /></a>',
					},
					ILucideLink2: true,
					ILucideArrowUpRight: true,
				},
			},
		});
		await flushPromises();

		expect(wrapper.text()).toContain('友人帐');
		expect(wrapper.text()).toContain('大佬们');
		expect(wrapper.text()).toContain('同行者');
		expect(wrapper.text()).toContain('静待有缘者。');
		expect(wrapper.text()).toContain('开放友链申请，请至');
		expect(wrapper.text()).toContain('留言板');
		expect(wrapper.text()).toContain('申请时请提供以下信息：');
		expect(wrapper.text()).toContain('网站地址（必须使用 HTTPS）');
		expect(wrapper.text()).toContain(
			'头像图片或网站代表图地址（可选，建议使用清晰的正方形图片）',
		);
		expect(wrapper.text()).toContain('聆听心中最初的声音');
		expect(wrapper.text()).toContain('本站的部分信息：');
		expect(wrapper.find('a[href="https://blog.example.com"]').exists()).toBe(true);
		expect(wrapper.find('a[href="https://img.example.com/site-logo.png"]').exists()).toBe(true);
		expect(wrapper.find('a[href="https://example.com"]').attributes('target')).toBe('_blank');
		expect(wrapper.find('a[href="https://example.com"]').attributes('rel')).toBe(
			'noopener noreferrer',
		);
		expect(wrapper.find('a[href="/messages"]').exists()).toBe(true);
		expect(wrapper.find('a[href="/messages"]').classes()).toContain(
			'friend-links-application__message-link',
		);
		expect(loadFriendLinks).toHaveBeenCalled();
	});

	it('offers retry when loading fails', async () => {
		const loadFriendLinks = vi.fn();
		vi.mocked(usePublicFriendLinks).mockReturnValue({
			siteProfile: ref(null),
			sections: ref([]),
			loading: ref(false),
			loadFailed: ref(true),
			loadFriendLinks,
		});
		const wrapper = mount(LinkPage, {
			global: {
				stubs: {
					ElButton: defineComponent({
						emits: ['click'],
						template: '<button @click="$emit(\'click\')"><slot /></button>',
					}),
				},
			},
		});

		await wrapper.get('button').trigger('click');

		expect(wrapper.text()).toContain('友链加载失败');
		expect(loadFriendLinks).toHaveBeenCalled();
	});
});
