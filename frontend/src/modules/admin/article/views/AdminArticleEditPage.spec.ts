import { flushPromises, shallowMount } from '@vue/test-utils';
import { reactive, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { ArticleUpsertRequestFilter } from '../composables/useAdminArticleForm';
import AdminArticleEditPage from './AdminArticleEditPage.vue';

const router = vi.hoisted(() => ({ push: vi.fn(), replace: vi.fn() }));
const route = vi.hoisted(() => ({ params: {} as { articleId?: string } }));
const routeLeave = vi.hoisted(() => ({ guard: undefined as (() => Promise<boolean>) | undefined }));
const confirmLeave = vi.hoisted(() => vi.fn());
const createArticle = vi.hoisted(() => vi.fn());

let upsertRequest: ArticleUpsertRequestFilter;

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
			handleUpdateArticle: vi.fn(),
		};
	},
}));

vi.mock('../../category/composables/useAdminCategoryList', () => ({
	useAdminCategoryList: () => ({
		categoryList: ref([]),
		getCategoryList: vi.fn().mockResolvedValue(undefined),
	}),
}));

vi.mock('../../tag/composables/useAdminTagList', () => ({
	useAdminTagList: () => ({
		tagList: ref([]),
		getTagList: vi.fn().mockResolvedValue(undefined),
	}),
}));

vi.mock('../composables/useAdminArticleDetail', () => ({
	useAdminArticleDetail: () => ({
		articleDetail: reactive({}),
		handleGetArticleDetails: vi.fn(),
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
	const wrapper = shallowMount(AdminArticleEditPage);
	await flushPromises();
	return wrapper;
}

describe('AdminArticleEditPage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		route.params = {};
		routeLeave.guard = undefined;
		createArticle.mockResolvedValue(true);
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
});
