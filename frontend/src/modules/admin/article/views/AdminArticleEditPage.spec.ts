import { flushPromises, shallowMount } from '@vue/test-utils';
import { defineComponent, reactive, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { ArticleUpsertRequestFilter } from '../composables/useAdminArticleForm';
import AdminArticleEditPage from './AdminArticleEditPage.vue';
import type { AdminCategoryItem } from '../../category/types/adminCategory';
import type { AdminTagItem } from '../../tag/types/adminTag';

const router = vi.hoisted(() => ({ push: vi.fn(), replace: vi.fn() }));
const route = vi.hoisted(() => ({ params: {} as { articleId?: string } }));
const routeLeave = vi.hoisted(() => ({ guard: undefined as (() => Promise<boolean>) | undefined }));
const confirmLeave = vi.hoisted(() => vi.fn());
const createArticle = vi.hoisted(() => vi.fn());
const uploadAdminImage = vi.hoisted(() => vi.fn());
const editorInsert = vi.hoisted(() => vi.fn());
const previewArticle = vi.hoisted(() => vi.fn());

vi.mock('../api/adminArticleApi', () => ({ previewArticle }));

let upsertRequest: ArticleUpsertRequestFilter;
let categories: AdminCategoryItem[] = [];
let tags: AdminTagItem[] = [];
let existingArticle: Record<string, unknown> = {};
const updateArticle = vi.hoisted(() => vi.fn());

vi.mock('vue-router', () => ({
	useRoute: () => route,
	useRouter: () => router,
	onBeforeRouteLeave: (guard: () => Promise<boolean>) => {
		routeLeave.guard = guard;
	},
}));

vi.mock('../composables/useAdminArticleForm', () => ({
	useAdminArticleForm: () => {
		const initUpsertRequest: ArticleUpsertRequestFilter = {
			title: '',
			summary: '',
			contentMd: '',
			categoryId: null,
			tagIds: [],
			coverUrl: '',
			isTop: false,
			status: 'DRAFT',
			allowComment: true,
		};
		upsertRequest = reactive({ ...initUpsertRequest });

		return {
			submitting: ref(false),
			initUpsertRequest,
			upsertRequest,
			rules: {},
			handleCreateArticle: createArticle,
			handleUpdateArticle: updateArticle,
		};
	},
}));

vi.mock('../../category/composables/useAdminCategoryList', () => ({
	useAdminCategoryList: () => ({
		categoryList: ref(categories),
		getCategoryList: vi.fn().mockResolvedValue(undefined),
	}),
}));

vi.mock('../../tag/composables/useAdminTagList', () => ({
	useAdminTagList: () => ({
		tagList: ref(tags),
		getTagList: vi.fn().mockResolvedValue(undefined),
	}),
}));

vi.mock('../composables/useAdminArticleDetail', () => ({
	useAdminArticleDetail: () => ({
		articleDetail: reactive(existingArticle),
		handleGetArticleDetails: vi.fn().mockResolvedValue(true),
	}),
}));

vi.mock('@/composables/useTheme', () => ({
	useTheme: () => ({ resolvedTheme: ref('light') }),
}));

vi.mock('@/modules/admin/file/composables/useAdminImageUpload', () => ({
	useAdminImageUpload: () => ({
		imageUploading: ref(false),
		handleUploadAdminImage: uploadAdminImage,
	}),
}));

vi.mock('md-editor-v3', () => ({
	config: vi.fn(),
	MdEditor: {
		name: 'MdEditor',
		emits: ['onUploadImg'],
		methods: { insert: editorInsert },
		setup() {
			return { getEditorView: () => ({ state: { selection: {} }, dispatch: vi.fn() }) };
		},
		template: '<div />',
	},
}));

vi.mock('element-plus', async (importOriginal) => ({
	...(await importOriginal<typeof import('element-plus')>()),
	ElMessage: { error: vi.fn() },
	ElMessageBox: { confirm: confirmLeave },
}));

async function mountPage() {
	const wrapper = shallowMount(AdminArticleEditPage, {
		global: {
			renderStubDefaultSlot: true,
			stubs: {
				MdEditor: false,
				ElForm: defineComponent({
					template: '<form><slot /></form>',
					methods: { validate: () => Promise.resolve(true) },
				}),
				ElTreeSelect: defineComponent({
					name: 'ElTreeSelect',
					props: ['data'],
					template: '<div />',
				}),
				AppTagCapsule: defineComponent({
					name: 'AppTagCapsule',
					props: ['name'],
					template: '<span>{{ name }}</span>',
				}),
			},
		},
	});
	await flushPromises();
	return wrapper;
}

describe('AdminArticleEditPage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		uploadAdminImage.mockReset();
		route.params = {};
		routeLeave.guard = undefined;
		createArticle.mockResolvedValue(true);
		updateArticle.mockResolvedValue(true);
		previewArticle.mockResolvedValue({ contentHtml: '<h1>未保存正文</h1>' });
		categories = [];
		tags = [];
		existingArticle = {};
	});

	it('inserts successful uploads as HTML and completes the upload without default Markdown images', async () => {
		const files = [new File(['image'], '架构图.png'), new File(['image'], '上传失败.png')];
		uploadAdminImage
			.mockResolvedValueOnce({
				url: 'https://img.example.com/architecture.png',
				originalName: '架构图.png',
			})
			.mockResolvedValueOnce(undefined);
		const wrapper = await mountPage();
		const callback = vi.fn();

		wrapper.getComponent({ name: 'MdEditor' }).vm.$emit('onUploadImg', files, callback);
		await flushPromises();

		expect(uploadAdminImage).toHaveBeenNthCalledWith(1, 'ARTICLE_CONTENT', files[0]);
		expect(uploadAdminImage).toHaveBeenNthCalledWith(2, 'ARTICLE_CONTENT', files[1]);
		expect(callback).toHaveBeenCalledExactlyOnceWith([]);
		expect(editorInsert).toHaveBeenCalledTimes(1);
		expect(editorInsert.mock.calls[0]![0]('选中文本')).toEqual({
			targetValue:
				'<img src="https://img.example.com/architecture.png" alt="架构图.png" />\n\n',
			select: false,
		});
		wrapper.unmount();
	});

	it('does not change the editor when every upload fails', async () => {
		uploadAdminImage.mockResolvedValue(undefined);
		const wrapper = await mountPage();
		upsertRequest.contentMd = '已有正文';
		const callback = vi.fn();

		wrapper
			.getComponent({ name: 'MdEditor' })
			.vm.$emit('onUploadImg', [new File(['image'], '失败.png')], callback);
		await flushPromises();

		expect(upsertRequest.contentMd).toBe('已有正文');
		expect(editorInsert).not.toHaveBeenCalled();
		expect(callback).not.toHaveBeenCalled();
		wrapper.unmount();
	});

	it('loads a Markdown-only editor response and can preview it without stored HTML or plain text', async () => {
		route.params = { articleId: '40001' };
		const contentMd = '# 长文章\n\n' + '用于编辑的正文。'.repeat(10000);
		const editorFields = {
			title: '长文章',
			summary: '文章摘要',
			contentMd,
			categoryId: 21001,
			tagIds: [30001],
			status: 'PUBLISHED',
			isTop: true,
			allowComment: true,
			coverUrl: 'https://img.example.com/cover.png',
		};
		existingArticle = { id: 40001, ...editorFields };
		const wrapper = await mountPage();

		expect(upsertRequest).toMatchObject(editorFields);
		await wrapper.get('.admin-article-preview-button').trigger('click');
		await flushPromises();
		expect(previewArticle).toHaveBeenCalledExactlyOnceWith(contentMd);
		expect(await routeLeave.guard?.()).toBe(true);
		expect(confirmLeave).not.toHaveBeenCalled();
		wrapper.unmount();
	});

	it('opens actual preview without saving or clearing unsaved changes', async () => {
		const wrapper = await mountPage();
		upsertRequest.title = '未保存标题';
		upsertRequest.contentMd = '# 未保存正文';
		await wrapper.get('.admin-article-preview-button').trigger('click');
		await flushPromises();

		expect(previewArticle).toHaveBeenCalledExactlyOnceWith('# 未保存正文');
		expect(
			wrapper.getComponent({ name: 'AdminArticlePreviewDialog' }).props('article'),
		).toMatchObject({
			title: '未保存标题',
			contentHtml: '<h1>未保存正文</h1>',
		});
		expect(createArticle).not.toHaveBeenCalled();
		expect(updateArticle).not.toHaveBeenCalled();
		confirmLeave.mockRejectedValueOnce(new Error('cancel'));
		expect(await routeLeave.guard?.()).toBe(false);
		expect(confirmLeave).toHaveBeenCalled();
		wrapper.unmount();
	});

	it('allows leaving without confirmation when the form is unchanged', async () => {
		await mountPage();

		expect(await routeLeave.guard?.()).toBe(true);
		expect(confirmLeave).not.toHaveBeenCalled();
	});

	it('allows leaving after the administrator confirms unsaved changes', async () => {
		await mountPage();
		upsertRequest.title = '尚未保存的标题';
		confirmLeave.mockResolvedValue(undefined);

		expect(await routeLeave.guard?.()).toBe(true);
		expect(confirmLeave).toHaveBeenCalledTimes(1);
	});

	it('cancels navigation when the administrator keeps editing', async () => {
		await mountPage();
		upsertRequest.title = '尚未保存的标题';
		confirmLeave.mockRejectedValue('cancel');

		expect(await routeLeave.guard?.()).toBe(false);
	});

	it('updates the saved snapshot after creating an article', async () => {
		const wrapper = await mountPage();
		Object.assign(upsertRequest, {
			title: '已保存的文章',
			contentMd: '文章正文',
			categoryId: 21001,
		});

		await wrapper.get('.admin-article-submit-button').trigger('click');
		await flushPromises();

		expect(createArticle).toHaveBeenCalledTimes(1);
		expect(router.push).toHaveBeenCalledWith('/admin/articles');
		expect(await routeLeave.guard?.()).toBe(true);
		expect(confirmLeave).not.toHaveBeenCalled();
	});

	it.each(['create', 'edit'])(
		'labels disabled options and allows saving them in %s mode',
		async (mode) => {
			const child: AdminCategoryItem = {
				id: 21001,
				parentId: 20001,
				level: 2,
				name: 'Java',
				status: 'ENABLED',
				description: '',
				sortNo: 0,
				articleCount: 0,
				createdAt: '',
				children: [],
			};
			categories = [
				{
					...child,
					id: 20001,
					parentId: null,
					level: 1,
					name: '技术',
					status: 'DISABLED',
					children: [child],
				},
			];
			tags = [
				{
					id: 30001,
					name: 'JWT',
					description: '',
					status: 'DISABLED',
					articleCount: 0,
					createdAt: '',
				},
			];
			if (mode === 'edit') {
				route.params = { articleId: '40001' };
				existingArticle = {
					title: '原文章',
					contentMd: '原正文',
					categoryId: 21001,
					tagIds: [30001],
					status: 'PUBLISHED',
				};
			}
			const wrapper = await mountPage();
			Object.assign(upsertRequest, {
				title: '文章',
				contentMd: '正文',
				categoryId: 21001,
				tagIds: [30001],
			});
			await wrapper.vm.$nextTick();
			const tree = wrapper.findComponent({ name: 'ElTreeSelect' });
			const options = tree.props('data');
			expect(options[0].name).toBe('技术（已禁用）');
			expect(options[0].disabled).toBe(true);
			expect(options[0].children[0].name).toBe('Java（已禁用）');
			expect(options[0].children[0].disabled).not.toBe(true);
			expect(wrapper.text()).toContain('该分类或父分类已禁用，文章发布后不会在前台显示。');
			expect(wrapper.text()).toContain('该标签不会在前台显示。');
			expect(wrapper.findComponent({ name: 'AppTagCapsule' }).props('name')).toBe(
				'JWT（已禁用）',
			);
			await wrapper.get('.admin-article-submit-button').trigger('click');
			await flushPromises();
			expect(mode === 'create' ? createArticle : updateArticle).toHaveBeenCalledTimes(1);
			wrapper.unmount();
		},
	);
});
