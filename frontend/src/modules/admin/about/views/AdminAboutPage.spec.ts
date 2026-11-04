import { flushPromises, mount } from '@vue/test-utils';
import { ref, toRef } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ElMessage } from 'element-plus';
import { uploadAdminImage } from '@/modules/admin/file/api/adminFileApi';
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
	ElMessage: { warning: vi.fn() },
}));

vi.mock('@/modules/admin/file/api/adminFileApi', () => ({
	uploadAdminImage: vi.fn(),
}));

vi.mock('md-editor-v3', () => ({
	MdEditor: {
		name: 'MdEditor',
		props: ['modelValue'],
		emits: ['update:modelValue', 'onUploadImg'],
		template: '<textarea :value="modelValue" />',
	},
}));

vi.mock('@/composables/useTheme', () => ({
	useTheme: () => ({ resolvedTheme: 'light' }),
}));

vi.mock('../composables/useAdminAboutPage', () => ({
	useAdminAboutPage: () => ({
		aboutPage: ref({
			updatedBy: { id: 10001, username: 'admin' },
			updatedAt: '2026-09-26T10:00:00Z',
		}),
		contentMd: toRef(pageState, 'contentMd'),
		loading: toRef(pageState, 'loading'),
		loadFailed: toRef(pageState, 'loadFailed'),
		submitting: toRef(pageState, 'submitting'),
		hasUnsavedChanges: toRef(pageState, 'hasUnsavedChanges'),
		getAdminAboutPage,
		saveAboutPage,
	}),
}));

function mountPage() {
	return mount(AdminAboutPage, {
		global: {
			stubs: {
				ElCard: { template: '<section><slot name="header" /><slot /></section>' },
				ElButton: {
					props: ['disabled', 'loading'],
					template:
						'<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
				},
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

	it('uploads about images and leaves title empty for manually entered captions', async () => {
		const file = new File(['image'], 'about.png', { type: 'image/png' });
		vi.mocked(uploadAdminImage).mockResolvedValueOnce({
			url: 'https://img.example.com/about/content/image.png',
			originalName: file.name,
			contentType: file.type,
			size: file.size,
		});
		const wrapper = mountPage();
		const callback = vi.fn();

		wrapper.getComponent({ name: 'MdEditor' }).vm.$emit('onUploadImg', [file], callback);
		await flushPromises();

		expect(uploadAdminImage).toHaveBeenCalledWith('ABOUT_CONTENT', file);
		expect(callback).toHaveBeenCalledExactlyOnceWith([
			{ url: 'https://img.example.com/about/content/image.png', alt: 'about.png', title: '' },
		]);
	});

	it('inserts only successful uploads when some files fail validation or uploading', async () => {
		vi.mocked(uploadAdminImage)
			.mockResolvedValueOnce({
				url: '/about/ok.png',
				originalName: 'ok.png',
				contentType: 'image/png',
				size: 1,
			})
			.mockRejectedValueOnce(new Error('upload failed'));
		const files = [
			new File(['1'], 'ok.png', { type: 'image/png' }),
			new File(['2'], 'failed.png', { type: 'image/png' }),
			new File(['3'], 'invalid.svg', { type: 'image/svg+xml' }),
		];
		const wrapper = mountPage();
		const callback = vi.fn();

		wrapper.getComponent({ name: 'MdEditor' }).vm.$emit('onUploadImg', files, callback);
		await flushPromises();

		expect(uploadAdminImage).toHaveBeenCalledTimes(2);
		expect(callback).toHaveBeenCalledExactlyOnceWith([
			{ url: '/about/ok.png', alt: 'ok.png', title: '' },
		]);
	});

	it('prevents saving and leaving until the pending upload finishes', async () => {
		pageState.hasUnsavedChanges = true;
		let resolveUpload!: (image: Awaited<ReturnType<typeof uploadAdminImage>>) => void;
		vi.mocked(uploadAdminImage).mockReturnValueOnce(
			new Promise((resolve) => {
				resolveUpload = resolve;
			}),
		);
		const wrapper = mountPage();
		const file = new File(['image'], 'about.png', { type: 'image/png' });

		wrapper.getComponent({ name: 'MdEditor' }).vm.$emit('onUploadImg', [file], vi.fn());
		await wrapper.vm.$nextTick();

		expect(wrapper.get('button').attributes('disabled')).toBeDefined();
		await expect(routeLeave.guard?.()).resolves.toBe(false);
		expect(ElMessage.warning).toHaveBeenCalledWith('图片上传中，请稍后再离开');
		expect(confirmLeave).not.toHaveBeenCalled();

		resolveUpload({
			url: '/about/image.png',
			originalName: file.name,
			contentType: file.type,
			size: file.size,
		});
		await flushPromises();
		expect(wrapper.get('button').attributes('disabled')).toBeUndefined();
	});
});
