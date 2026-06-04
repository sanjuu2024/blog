import { listArticles } from '../api/articleApi';
import type {
	ArticleFilterForm,
	ArticlePageParams,
	PublicArticleListItem,
	PublicArticleListQuery,
} from '../types/article';

const ARTICLE_PAGE_SIZE = 10;

export function useArticleList() {
	// 文章列表
	const articles = ref<PublicArticleListItem[]>([]);

	// 分页参数初始值
	const initPageParams: ArticlePageParams = {
		pageNum: 1,
		pageSize: ARTICLE_PAGE_SIZE,
		hasNext: false,
	};

	// 分页参数
	const pageParams = reactive<ArticlePageParams>({ ...initPageParams });

	// 查询参数
	const filterForm = reactive<ArticleFilterForm>({ ...createInitialFilterForm() });

	// 用于滚动页面自动获取接下来的分页列表
	const loading = ref(false);

	// 查询参数初始值
	function createInitialFilterForm(): ArticleFilterForm {
		return {
			categoryId: undefined,
			tagIds: [],
		};
	}

	// 重置筛选条件
	function resetFilterForm() {
		Object.assign(filterForm, createInitialFilterForm());
	}

	// 构造 获取已发表文章分页列表 请求参数
	function buildArticleListQueryParams() {
		const queryParams: PublicArticleListQuery = {
			pageNum: pageParams.pageNum,
			pageSize: pageParams.pageSize,
			categoryId: filterForm.categoryId,
			tagIds: filterForm.tagIds,
		};
		return queryParams;
	}

	// 发送 获取已发表文章分页列表 请求
	async function getPublishedArticles(pageNum?: number, pageSize?: number) {
		if (loading.value) {
			return false;
		}
		loading.value = true;

		if (pageNum) {
			pageParams.pageNum = pageNum;
		}
		if (pageSize) {
			pageParams.pageSize = pageSize;
		}

		try {
			const queryParams = buildArticleListQueryParams();
			const data = await listArticles(queryParams);
			articles.value.push(...data.records);
			pageParams.hasNext = data.hasNext;
			return true;
		} catch (error) {
			console.error('获取已发表文章分页列表失败:', error);
			return false;
		} finally {
			loading.value = false;
		}
	}

	// 重置分页后重新获取文章列表
	async function resetPageParamsAndGetPublishedArticles() {
		Object.assign(pageParams, initPageParams);
		articles.value = [];
		await getPublishedArticles();
	}

	// 滚动页面时调用
	async function loadMoreArticles() {
		if (loading.value || !pageParams.hasNext) return;

		pageParams.pageNum++;
		const success = await getPublishedArticles();
		if (!success) {
			pageParams.pageNum--; // “回滚”页码，防止后续请求跳页
		}
	}

	return {
		articles,
		pageParams,
		filterForm,
		loading,
		resetFilterForm,
		getPublishedArticles,
		loadMoreArticles,
		resetPageParamsAndGetPublishedArticles,
	};
}
