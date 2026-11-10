import { onScopeDispose, ref, watch, type ComputedRef } from 'vue';
import { getArchiveArticles } from '../api/archiveApi';
import type { ArchiveArticle, ArchiveQuery } from '../types/archive';

export function useArchiveArticles(query: ComputedRef<ArchiveQuery | null>, timeZone: string) {
	const articles = ref<ArchiveArticle[]>([]);
	const total = ref(0);
	const pageNum = ref(0);
	const hasNext = ref(false);
	const loading = ref(false);
	const loaded = ref(false);
	const loadFailed = ref(false);
	let controller: AbortController | null = null;

	async function loadPage(page: number) {
		if (!query.value || loading.value) {
			return;
		}

		const current = new AbortController();
		controller = current;
		loading.value = true;
		loadFailed.value = false;
		try {
			const data = await getArchiveArticles(
				{ ...query.value, timeZone, pageNum: page, pageSize: 10 },
				current.signal,
			);
			// 条件快速变化时，旧响应和旧 finally 均不能覆盖当前列表。
			if (controller !== current) {
				return;
			}

			articles.value = page === 1 ? data.records : [...articles.value, ...data.records];
			total.value = data.total;
			pageNum.value = data.pageNum;
			hasNext.value = data.hasNext;
			loaded.value = true;
		} catch {
			if (controller === current && !current.signal.aborted) {
				loadFailed.value = true;
			}
		} finally {
			if (controller === current) {
				loading.value = false;
			}
		}
	}

	// URL 规范化可能替换 query 对象；仅实际筛选条件变化时重置分页。
	watch(
		() => JSON.stringify(query.value),
		() => {
			controller?.abort();
			controller = null;
			loading.value = false;
			articles.value = [];
			total.value = 0;
			pageNum.value = 0;
			hasNext.value = false;
			loaded.value = false;
			loadFailed.value = false;
			void loadPage(1);
		},
		{ immediate: true },
	);

	function loadMore() {
		if (hasNext.value && !loadFailed.value) {
			void loadPage(pageNum.value + 1);
		}
	}

	function retry() {
		void loadPage(pageNum.value + 1);
	}

	onScopeDispose(() => {
		controller?.abort();
		controller = null;
	});

	return { articles, total, loading, loaded, loadFailed, hasNext, loadMore, retry };
}
