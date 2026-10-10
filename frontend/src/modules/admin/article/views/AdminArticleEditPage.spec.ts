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
		handleUploadAdminImage: vi.fn(),
	}),
}));

vi.mock('md-editor-v3', () => ({
	MdEditor: { template: '<div />' },
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
		route.params = {};
		routeLeave.guard = undefined;
		createArticle.mockResolvedValue(true);
		updateArticle.mockResolvedValue(true);
		categories = [];
		tags = [];
		existingArticle = {};
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

		await wrapper.get('.admin-article-edit-footer el-button-stub').trigger('click');
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
			await wrapper.get('.admin-article-edit-footer el-button-stub').trigger('click');
			await flushPromises();
			expect(mode === 'create' ? createArticle : updateArticle).toHaveBeenCalledTimes(1);
			wrapper.unmount();
		},
	);
});
