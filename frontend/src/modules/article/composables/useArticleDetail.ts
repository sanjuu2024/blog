import { ElMessage } from 'element-plus';
import type { PublicArticleDetailData } from '../types/article';
import { getArticleDetails, likeArticle, unlikeArticle } from '../api/articleApi';
import { isAxiosError } from 'axios';
import type { ApiResult } from '@/types/api';
import { applyPageSeo, resetPageSeo } from '@/modules/seo/utils/pageSeo';
import { onScopeDispose, getCurrentScope } from 'vue';

export function useArticleDetail() {
	// 文章详情
	const article = ref<PublicArticleDetailData | null>(null);

	// 文章是否加载中
	const isLoading = ref(false);

	// 文章详情请求失败的错误信息
	const errorMessage = ref('');
	const updatingLike = ref(false);
	let requestVersion = 0;
	// 离开文章页后失效未完成请求，防止旧文章标签覆盖新页面。
	if (getCurrentScope()) {
		onScopeDispose(() => requestVersion++);
	}

	// 发送 获取文章详情 请求
	async function getArticleDetail(articleId: string | string[] | number) {
		const version = ++requestVersion;
		const rawId = Array.isArray(articleId) ? articleId[0] : articleId;
		const id = Number(rawId);

		if (!Number.isSafeInteger(id) || id <= 0) {
			article.value = null;
			errorMessage.value = '文章 ID 不合法';
			resetPageSeo(`文章 ID 不合法 - ${import.meta.env.VITE_APP_TITLE || '青禾边'}`);
			isLoading.value = false;
			ElMessage.error('文章 ID 不合法');
			return;
		}

		isLoading.value = true;
		article.value = null;
		errorMessage.value = '';

		try {
			const result = await getArticleDetails(id);
			// 快速跨文章导航时仅应用当前详情及 SEO，不再发送独立元信息请求。
			if (version !== requestVersion) return;
			article.value = result;
			applyPageSeo(result.seo);
		} catch (err) {
			if (version !== requestVersion) return;
			if (isAxiosError<ApiResult>(err) && err.response?.status === 404) {
				errorMessage.value = '文章不存在';
			} else if (isAxiosError(err) && err.request && !err.response) {
				errorMessage.value = '网络异常，文章加载失败';
			} else {
				errorMessage.value = '文章加载失败，请稍后重试';
			}
			resetPageSeo(`${errorMessage.value} - ${import.meta.env.VITE_APP_TITLE || '青禾边'}`);
		} finally {
			if (version === requestVersion) isLoading.value = false;
		}
	}

	// 点赞请求完成后再更新页面状态，避免失败时按钮和计数与服务端不一致
	async function toggleArticleLike() {
		if (!article.value || updatingLike.value) return;

		const articleId = article.value.id;
		updatingLike.value = true;
		try {
			const result = await (article.value.liked
				? unlikeArticle(articleId)
				: likeArticle(articleId));
			if (article.value?.id === articleId) {
				article.value.liked = result.liked;
				article.value.likeCount = result.likeCount;
			}
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		} finally {
			updatingLike.value = false;
		}
	}

	return {
		article,
		isLoading,
		errorMessage,
		updatingLike,
		getArticleDetail,
		toggleArticleLike,
	};
}
