import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent, h, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { listAdminFriendLinks } from '../api/adminFriendLinkApi';
import { FRIEND_LINK_SECTION, FRIEND_LINK_STATUS } from '../types/adminFriendLink';
import AdminFriendLinkListPage from './AdminFriendLinkListPage.vue';

vi.mock('../api/adminFriendLinkApi', () => ({
	listAdminFriendLinks: vi.fn(),
	deleteAdminFriendLink: vi.fn(),
	updateAdminFriendLink: vi.fn(),
	createAdminFriendLink: vi.fn(),
}));

vi.mock('../composables/useAdminFriendLinkForm', () => ({
	useAdminFriendLinkForm: () => ({
		drawerVisible: ref(false),
		drawerMode: ref('create'),
		submitting: ref(false),
		form: ref({
			name: '',
			url: '',
			logoUrl: '',
			description: '',
			section: 'FRIENDS',
			sortNo: 100,
			status: 'ENABLED',
		}),
		rules: {},
		setFormRef: vi.fn(),
		openCreateDrawer: vi.fn(),
		openEditDrawer: vi.fn(),
		submitForm: vi.fn(),
	}),
}));

const TableColumnStub = defineComponent({
	setup(_, { slots }) {
		return () =>
			h(
				'div',
				slots.default?.({
					row: {
						id: 110001,
						name: 'Example',
						url: 'https://example.com',
						logoUrl: '',
						description: 'site',
						section: FRIEND_LINK_SECTION.FRIENDS,
						sortNo: 100,
						status: FRIEND_LINK_STATUS.ENABLED,
						createdAt: '',
					},
				}),
			);
	},
});

describe('AdminFriendLinkListPage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(listAdminFriendLinks).mockResolvedValue({
			records: [
				{
					id: 110001,
					name: 'Example',
					url: 'https://example.com',
					logoUrl: '',
					description: 'site',
					section: FRIEND_LINK_SECTION.FRIENDS,
					sortNo: 100,
					status: FRIEND_LINK_STATUS.ENABLED,
					createdAt: '',
					updatedAt: '',
				},
			],
			pageNum: 1,
			pageSize: 10,
			total: 1,
			totalPages: 1,
			hasNext: false,
		});
	});

	it('renders fixed sections, status, and sort controls from loaded records', async () => {
		const wrapper = mount(AdminFriendLinkListPage, {
			global: {
				stubs: {
					ElCard: { template: '<section><slot name="header"/><slot/></section>' },
					ElForm: { template: '<form><slot/></form>' },
					ElFormItem: { template: '<div><slot/></div>' },
					ElInput: true,
					ElInputNumber: true,
					ElSelect: { template: '<div><slot/></div>' },
					ElOption: true,
					ElRadioGroup: { template: '<div><slot/></div>' },
					ElRadio: { template: '<label><slot/></label>' },
					ElButton: { template: '<button><slot/></button>' },
					ElTable: { template: '<div><slot/></div>' },
					ElTableColumn: TableColumnStub,
					ElTag: { template: '<span><slot/></span>' },
					ElPagination: true,
					ElDrawer: {
						template: '<div><slot/><slot name="header"/><slot name="footer"/></div>',
					},
					ElPopconfirm: { template: '<div><slot name="reference"/></div>' },
					ILucidePlus: true,
				},
			},
		});
		await flushPromises();

		expect(listAdminFriendLinks).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 });
		expect(wrapper.text()).toContain('友链管理');
		expect(wrapper.text()).toContain('友人帐');
		expect(wrapper.text()).toContain('全部');
		expect(wrapper.text()).toContain('100');
		expect(wrapper.text()).toContain('启用');
	});
});
