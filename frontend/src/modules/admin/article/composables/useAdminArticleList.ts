import { listArticles } from '../api/adminArticleApi';
import type {
	AdminArticleListQuery,
	AdminArticleListItem,
	AdminArticlePageParams,
	AdminArticleFilterForm,
	AdminArticlePageData,
	AdminArticlePageMeta,
} from '../types/adminArticle';

export function useAdminArticleList() {
	// 数据列表
	const articleList = ref<AdminArticleListItem[]>([]);

	// 分页元信息
	const pageMeta = reactive({
		total: 0,
		totalPages: 0,
		hasNext: false,
	} as AdminArticlePageMeta);

	// 分页参数初始值
	const initPageParams: AdminArticlePageParams = {
		pageNum: 1,
		pageSize: 7,
	};

	// 分页参数
	const pageParams = reactive<AdminArticlePageParams>({ ...initPageParams });

	// 条件参数初始值
	const initFilterForm: AdminArticleFilterForm = {
		title: '',
		categoryId: undefined,
		status: 'ALL',
		isTop: 'ALL',
	};

	// 条件参数
	const filterForm = reactive<AdminArticleFilterForm>({ ...initFilterForm });

	// 重置条件参数
	function resetFilterForm() {
		Object.assign(filterForm, initFilterForm);
		getArticleList();
	}

	// 构造查询参数（合并条件参数和分页参数）
	function buildArticleListQuery(): AdminArticleListQuery {
		return {
			pageNum: pageParams.pageNum,
			pageSize: pageParams.pageSize,
			title: filterForm.title?.trim() || undefined,
			categoryId: filterForm.categoryId,
			status: filterForm.status === 'ALL' ? undefined : filterForm.status,
			isTop: filterForm.isTop === 'ALL' ? undefined : filterForm.isTop,
		};
	}

	// 获取文章列表
	async function getArticleList() {
		try {
			const data: AdminArticlePageData = await listArticles(buildArticleListQuery());
			articleList.value = data.records;
			pageParams.pageNum = data.pageNum;
			pageParams.pageSize = data.pageSize;
			pageMeta.total = data.total;
			pageMeta.totalPages = data.totalPages;
			pageMeta.hasNext = data.hasNext;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	return {
		articleList,
		pageMeta,
		pageParams,
		initFilterForm,
		filterForm,
		buildArticleListQuery,
		getArticleList,
		resetFilterForm,
	};
}
