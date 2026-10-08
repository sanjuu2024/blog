import { mount } from '@vue/test-utils';
import { computed, reactive, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAdminSecurityEventList } from '../composables/useAdminSecurityEventList';
import type { SecurityEventFilterForm, SecurityEventItem } from '../types/adminSecurityEvent';
import AdminSecurityEventPage from './AdminSecurityEventPage.vue';

vi.mock('@vueuse/core', () => ({
	useMediaQuery: () => computed(() => false),
}));

vi.mock('../composables/useAdminSecurityEventList', () => ({
	useAdminSecurityEventList: vi.fn(),
}));

describe('AdminSecurityEventPage', () => {
	let event: SecurityEventItem;

	beforeEach(() => {
		event = {
			id: 1,
			eventType: 'LOGIN',
			outcome: 'FAILURE',
			userId: 10001,
			userUsername: 'alice',
			userNickname: '测试用户',
			userDeleted: false,
			actorId: null,
			actorUsername: null,
			actorNickname: null,
			actorDeleted: null,
			account: 'alice@example.com',
			ip: '192.0.2.10',
			userAgent: 'full-browser-agent',
			requestMethod: 'POST',
			requestPath: '/api/v1/auth/login',
			browser: 'Chrome',
			operatingSystem: 'Windows',
			device: '桌面设备',
			description: '密码错误',
			createdAt: '2026-10-08T00:00:00Z',
		};
		vi.mocked(useAdminSecurityEventList).mockReturnValue({
			securityEventList: ref<SecurityEventItem[]>([event]),
			loading: ref(false),
			pageMeta: reactive({ total: 1, totalPages: 1, hasNext: false }),
			pageParams: reactive({ pageNum: 1, pageSize: 10 }),
			filterForm: reactive<SecurityEventFilterForm>({
				eventType: '',
				outcome: '',
				userId: undefined,
				createdAtRange: [],
			}),
			getSecurityEventList: vi.fn(),
			resetFilterForm: vi.fn(),
		});
	});

	function mountPage() {
		return mount(AdminSecurityEventPage, {
			global: {
				stubs: {
					ElCard: { template: '<div><slot name="header" /><slot /></div>' },
					ElForm: { template: '<form><slot /></form>' },
					ElFormItem: { template: '<div><slot /></div>' },
					ElSelect: true,
					ElOption: true,
					ElInputNumber: true,
					ElDatePicker: true,
					ElButton: { template: '<button><slot /></button>' },
					ElTable: { template: '<div><slot /></div>' },
					ElTableColumn: {
						data: () => ({ row: event }),
						template: '<div><slot :row="row" /></div>',
					},
					ElTag: { template: '<span><slot /></span>' },
					ElTooltip: {
						props: ['content'],
						template: '<div :title="content"><slot /></div>',
					},
					ElPagination: true,
				},
			},
		});
	}

	it('renders readable source and allows viewing the full User-Agent', () => {
		const wrapper = mountPage();

		expect(wrapper.text()).toContain('安全事件');
		expect(wrapper.findAll('button')).toHaveLength(2);
		expect(wrapper.text()).toContain('目标用户：测试用户');
		expect(wrapper.text()).toContain('（alice）');
		expect(wrapper.text()).toContain('ID：10001');
		expect(wrapper.text()).not.toContain('alice@example.com');
		expect(wrapper.text()).toContain('192.0.2.10');
		expect(wrapper.text()).toContain('Chrome / Windows / 桌面设备');
		expect(wrapper.text()).toContain('POST /api/v1/auth/login');
		expect(wrapper.find('[title="full-browser-agent"]').exists()).toBe(true);
		expect(wrapper.text()).not.toContain('操作者：');
	});

	it('shows missing historical metadata without inventing an identity', () => {
		event.userId = null;
		event.account = null;
		event.ip = null;
		event.userAgent = null;
		event.requestMethod = null;
		event.requestPath = null;
		const wrapper = mountPage();

		expect(wrapper.text()).toContain('未关联用户');
		expect(wrapper.text()).not.toContain('账号：未记录');
		expect(wrapper.text()).toContain('IP: 未记录');
		expect(wrapper.text()).toContain('客户端: 未记录');
	});

	it('shows the administrator only for operations with an actor', () => {
		event.eventType = 'USER_STATUS_CHANGE';
		event.account = null;
		event.actorId = 1;
		event.actorUsername = 'admin';
		event.actorNickname = '站点管理员';
		event.actorDeleted = false;
		const wrapper = mountPage();

		expect(wrapper.text()).toContain('操作者：站点管理员');
		expect(wrapper.text()).toContain('（admin）');
		expect(wrapper.text()).toContain('ID：1');
		expect(wrapper.text()).not.toContain('账号：未记录');
	});

	it('shows account input for unmatched login and email for verification', () => {
		event.userId = null;
		event.description = '账号不存在';
		const login = mountPage();
		expect(login.text()).toContain('账号：alice@example.com');
		expect(login.text()).toContain('账号不存在');
		login.unmount();

		event.eventType = 'EMAIL_VERIFICATION';
		event.description = '验证码发送成功';
		const verification = mountPage();
		expect(verification.text()).toContain('邮箱：alice@example.com');
		expect(verification.text()).not.toContain('账号不存在');
	});

	it('shows deleted users and their current placeholder usernames with deleted styling', () => {
		event.userDeleted = true;
		event.userUsername = 'deleted_10001';
		event.userNickname = null;
		event.actorId = 1;
		event.actorUsername = 'deleted_1';
		event.actorDeleted = true;
		const wrapper = mountPage();

		expect(wrapper.text()).toContain('目标用户：账号已注销');
		expect(wrapper.text()).toContain('操作者：账号已注销');
		expect(wrapper.text()).toContain('（deleted_10001）');
		expect(wrapper.text()).toContain('（deleted_1）');
		expect(wrapper.findAll('.security-event-deleted')).toHaveLength(2);
		expect(wrapper.text()).toContain('ID：10001');
	});

	it('preserves the target ID when its user record is missing', () => {
		event.userUsername = null;
		event.userNickname = null;
		event.userDeleted = null;
		const wrapper = mountPage();

		expect(wrapper.text()).toContain('目标用户：用户资料不存在');
		expect(wrapper.text()).toContain('ID：10001');
	});
});
