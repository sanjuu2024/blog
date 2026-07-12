import { listArticles } from '@/modules/article/api/articleApi';
import type {
	PublicArticleListItem,
	PublicArticleListQuery,
} from '@/modules/article/types/article';

export function useHomePage() {
	// （最多）前四篇置顶文章列表
	const loadingTopArticles = ref(false);
	const topArticlesLoadFailed = ref(false);
	const topArticlesLoaded = ref(false);
	const topArticles = ref<PublicArticleListItem[]>([]);

	// （最多）前四篇最新文章列表
	const loadingLatestArticles = ref(false);
	const latestArticlesLoadFailed = ref(false);
	const latestArticlesLoaded = ref(false);
	const latestArticles = ref<PublicArticleListItem[]>([]);

	// 发送 获取（最多）前四篇置顶文章列表 请求
	async function getTopArticles() {
		if (loadingTopArticles.value) return;

		loadingTopArticles.value = true;
		topArticlesLoadFailed.value = false;
		const queryParams: PublicArticleListQuery = {
			pageNum: 1,
			pageSize: 4,
			isTop: true,
		};

		try {
			const data = await listArticles({
				...queryParams,
			});
			topArticles.value = data.records;
		} catch {
			topArticles.value = [];
			topArticlesLoadFailed.value = true;
		} finally {
			topArticlesLoaded.value = true;
			loadingTopArticles.value = false;
		}
	}

	// 发送 获取（最多）前四篇最新文章列表 请求
	async function getLatestArticles() {
		if (loadingLatestArticles.value) return;

		loadingLatestArticles.value = true;
		latestArticlesLoadFailed.value = false;
		const queryParams: PublicArticleListQuery = {
			pageNum: 1,
			pageSize: 4,
			sort: 'LATEST',
		};

		try {
			const data = await listArticles({
				...queryParams,
			});
			latestArticles.value = data.records;
		} catch {
			latestArticles.value = [];
			latestArticlesLoadFailed.value = true;
		} finally {
			latestArticlesLoaded.value = true;
			loadingLatestArticles.value = false;
		}
	}

	return {
		loadingTopArticles,
		topArticlesLoadFailed,
		topArticlesLoaded,
		topArticles,
		loadingLatestArticles,
		latestArticlesLoadFailed,
		latestArticlesLoaded,
		latestArticles,
		getTopArticles,
		getLatestArticles,
	};
}
