import axios from 'axios';
import { listArticles } from '../api/articleApi';
import type {
	ArticleFilterForm,
	ArticlePageParams,
	PublicArticleListItem,
	PublicArticleListQuery,
} from '../types/article';

const ARTICLE_PAGE_SIZE = 10;

type ArticleRequestResult = 'success' | 'failed' | 'canceled' | 'skipped';
// success 成功，failed 失败，canceled 被取消，skipped 因为已有请求进行中而跳过

interface GetPublishedArticlesOptions {
	// 控制请求行为的配置：
	// replace 表示新数据是替换列表，不是追加。
	// cancelPrevious 表示发新请求前，先取消旧请求。
	replace?: boolean;
	cancelPrevious?: boolean;
}

export function useArticleList() {
	// 🔺注意 articleAbortController 是 let，可变
	let articleListAbortController: AbortController | null = null;

	// 文章列表
	const articles = ref<PublicArticleListItem[]>([]);
	// 当前查询条件命中的文章总数
	const total = ref(0);
	// 是否已经成功取得当前查询条件的第一页，避免加载期间误显示空结果
	const hasLoaded = ref(false);

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
			keyword: '',
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
			keyword: filterForm.keyword || undefined,
			categoryId: filterForm.categoryId,
			tagIds: filterForm.tagIds,
		};
		return queryParams;
	}

	// 🍉发送 获取已发表文章分页列表 请求
	async function getPublishedArticles(
		pageNum?: number,
		pageSize?: number,
		queryParams?: PublicArticleListQuery,
		options: GetPublishedArticlesOptions = {},
	): Promise<ArticleRequestResult> {
		// 当前请求正在进行中，且不允许取消前一个请求时，直接跳过本次请求
		if (loading.value && !options.cancelPrevious) {
			return 'skipped';
		}

		// 配置允许取消前一个请求。快速切换分类时，旧分类请求就不会继续干扰新分类数据。
		if (options.cancelPrevious) {
			articleListAbortController?.abort(); // 取消前一个请求
		}

		// 给当前这次请求创建一个新的取消开关，并把它记录到 articleListAbortController 中，这样下次如果要取消可以直接调用
		const abortController = new AbortController();
		articleListAbortController = abortController;
		loading.value = true;

		if (pageNum) pageParams.pageNum = pageNum;
		if (pageSize) pageParams.pageSize = pageSize;

		try {
			if (queryParams === undefined) queryParams = buildArticleListQueryParams();
			const data = await listArticles(queryParams, {
				// 把 signal 交给 Axios。这样这个请求之后才真的能被 abort() 取消
				signal: abortController.signal,
			});

			if (articleListAbortController !== abortController) {
				// 如果当前请求已经不是最新请求，就不要把它返回的数据写进页面。（防止旧请求的响应代替新请求的响应数据污染页面）
				return 'canceled';
			}

			// 如果配置要求替换，则替换，否则是追加（比如 loadMOre 时就不用 replace，直接追加）
			articles.value = options.replace ? data.records : [...articles.value, ...data.records];
			total.value = data.total;
			hasLoaded.value = true;
			pageParams.hasNext = data.hasNext;

			return 'success';
		} catch (error) {
			// 错误提示已经由 request 响应拦截器统一处理
			if (axios.isCancel(error) || abortController.signal.aborted) {
				// 如果错误是“请求被取消”，就返回 canceled，不把它当成真正失败。
				return 'canceled';
			}

			return 'failed';
		} finally {
			// 只有“当前最新请求”才能关闭 loading。这样旧请求被取消后，它的 finally 不会误把新请求的 loading 关掉
			if (articleListAbortController === abortController) {
				articleListAbortController = null;
				loading.value = false;
			}
		}
	}

	// 🍉重置分页后重新获取文章列表
	async function resetPageParamsAndGetPublishedArticles() {
		Object.assign(pageParams, initPageParams);
		articles.value = [];
		total.value = 0;
		hasLoaded.value = false;

		await getPublishedArticles(undefined, undefined, undefined, {
			replace: true, // 不是追加而是完全替换
			cancelPrevious: true, // 允许取消上一个请求
		});
	}

	// 🍉滚动页面时调用。加载更多时不取消就请求，只防重复
	async function loadMoreArticles() {
		if (loading.value || !pageParams.hasNext) return;

		const nextPageNum = pageParams.pageNum + 1;
		pageParams.pageNum = nextPageNum;

		const res = await getPublishedArticles();
		if (res === 'failed' && pageParams.pageNum === nextPageNum) {
			pageParams.pageNum--; // 回滚页码，防止后续请求跳页
		}
	}

	return {
		articles,
		total,
		hasLoaded,
		pageParams,
		filterForm,
		loading,
		resetFilterForm,
		getPublishedArticles,
		loadMoreArticles,
		resetPageParamsAndGetPublishedArticles,
	};
}
