import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminAboutPage from './AdminAboutPage.vue';

const routeLeave = vi.hoisted(() => ({
	guard: undefined as (() => Promise<boolean>) | undefined,
}));
const confirmLeave = vi.hoisted(() => vi.fn());
const pageState = vi.hoisted(() => ({
	contentMd: '# 关于本站',
	hasUnsavedChanges: false,
	loading: false,
	loadFailed: false,
	submitting: false,
}));
const getAdminAboutPage = vi.hoisted(() => vi.fn());
const saveAboutPage = vi.hoisted(() => vi.fn());

vi.mock('vue-router', () => ({
	onBeforeRouteLeave: (guard: () => Promise<boolean>) => {
		routeLeave.guard = guard;
	},
}));

vi.mock('element-plus', async (importOriginal) => ({
	...(await importOriginal<typeof import('element-plus')>()),
	ElMessageBox: { confirm: confirmLeave },
}));

vi.mock('md-editor-v3', () => ({
	MdEditor: {
		props: ['modelValue'],
		emits: ['update:modelValue'],
		template: '<textarea :value="modelValue" />',
	},
}));

vi.mock('@/composables/useTheme', () => ({
	useTheme: () => ({ resolvedTheme: 'light' }),
}));

vi.mock('../composables/useAdminAboutPage', () => ({
	useAdminAboutPage: () => ({
		aboutPage: {
			value: {
				updatedBy: { id: 10001, username: 'admin' },
				updatedAt: '2026-09-26T10:00:00Z',
			},
		},
		contentMd: {
			get value() {
				return pageState.contentMd;
			},
			set value(value: string) {
				pageState.contentMd = value;
			},
		},
		loading: {
			get value() {
				return pageState.loading;
			},
		},
		loadFailed: {
			get value() {
				return pageState.loadFailed;
			},
		},
		submitting: {
			get value() {
				return pageState.submitting;
			},
		},
		hasUnsavedChanges: {
			get value() {
				return pageState.hasUnsavedChanges;
			},
		},
		getAdminAboutPage,
		saveAboutPage,
	}),
}));

function mountPage() {
	return mount(AdminAboutPage, {
		global: {
			stubs: {
				ElCard: { template: '<section><slot name="header" /><slot /></section>' },
				ElButton: { template: '<button @click="$emit(\'click\')"><slot /></button>' },
				AppLoading: { template: '<div><slot /></div>' },
			},
		},
	});
}

describe('AdminAboutPage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		pageState.contentMd = '# 关于本站';
		pageState.hasUnsavedChanges = false;
		pageState.loading = false;
		pageState.loadFailed = false;
		pageState.submitting = false;
		routeLeave.guard = undefined;
		confirmLeave.mockResolvedValue(undefined);
	});

	it('loads the editor content on mount', async () => {
		mountPage();
		await flushPromises();
		expect(getAdminAboutPage).toHaveBeenCalledTimes(1);
	});

	it('asks for confirmation when leaving with unsaved changes', async () => {
		mountPage();
		pageState.hasUnsavedChanges = true;
		await expect(routeLeave.guard?.()).resolves.toBe(true);
		expect(confirmLeave).toHaveBeenCalledTimes(1);
	});
});
