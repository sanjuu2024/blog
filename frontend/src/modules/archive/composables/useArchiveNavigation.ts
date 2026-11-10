import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useEnabledCategoryList } from '@/modules/category/composables/useEnabledCategoryList';
import { useEnabledTagList } from '@/modules/tag/composables/useEnabledTagList';
import { getArchiveYears } from '../api/archiveApi';
import type { ArchiveMode, ArchiveQuery, ArchiveYear } from '../types/archive';

function parsePositiveInteger(value: unknown) {
	if (typeof value !== 'string' || !/^[1-9]\d*$/.test(value)) {
		return undefined;
	}

	const id = Number(value);
	return Number.isSafeInteger(id) ? id : undefined;
}

export function useArchiveNavigation() {
	const route = useRoute();
	const router = useRouter();
	// 汇总与分页共用命名时区，后端才能按浏览器当地日期归年并应用夏令时规则。
	const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
	const categories = useEnabledCategoryList();
	const tags = useEnabledTagList();
	const years = ref<ArchiveYear[]>([]);
	const yearsLoading = ref(false);
	const yearsLoaded = ref(false);
	const yearsFailed = ref(false);

	async function loadYears() {
		if (yearsLoading.value) {
			return;
		}

		yearsLoading.value = true;
		yearsFailed.value = false;
		try {
			years.value = await getArchiveYears(timeZone);
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理。
			yearsFailed.value = true;
		} finally {
			yearsLoaded.value = true;
			yearsLoading.value = false;
		}
	}

	onMounted(() => {
		void loadYears();
		void categories.getEnabledCategoryList();
		void tags.getEnabledTagList();
	});

	const mode = computed<ArchiveMode>(() =>
		route.query.mode === 'category' || route.query.mode === 'tag' ? route.query.mode : 'time',
	);
	// 只等待当前模式的导航数据，其他模式加载失败不阻塞当前列表。
	const ready = computed(() => {
		if (mode.value === 'time') {
			return yearsLoaded.value && !yearsFailed.value;
		}
		if (mode.value === 'category') {
			return categories.loaded.value && !categories.loadFailed.value;
		}
		return tags.loaded.value && !tags.loadFailed.value;
	});
	const allCategories = computed(() =>
		categories.enabledCategoryList.value.flatMap((item) => [item, ...item.children]),
	);
	const selectedCategory = computed(() =>
		allCategories.value.find(
			(item) => item.id === parsePositiveInteger(route.query.categoryId),
		),
	);
	const selectedTag = computed(() =>
		tags.enabledTagList.value.find(
			(item) => item.id === parsePositiveInteger(route.query.tagId),
		),
	);
	const selectedYear = computed(
		() =>
			years.value.find((item) => item.year === parsePositiveInteger(route.query.year)) ??
			years.value[0],
	);
	const query = computed<ArchiveQuery | null>(() => {
		if (!ready.value) {
			return null;
		}
		if (mode.value === 'time') {
			return selectedYear.value ? { year: selectedYear.value.year } : {};
		}
		if (mode.value === 'category') {
			return selectedCategory.value ? { categoryId: selectedCategory.value.id } : {};
		}
		return selectedTag.value ? { tagId: selectedTag.value.id } : {};
	});
	const title = computed(() => {
		if (mode.value === 'time') {
			return selectedYear.value ? `${selectedYear.value.year} 年` : '全部文章';
		}
		if (mode.value === 'tag') {
			return selectedTag.value ? `#${selectedTag.value.name}` : '全部文章';
		}

		const category = selectedCategory.value;
		if (!category) {
			return '全部文章';
		}

		const parent = allCategories.value.find((item) => item.id === category.parentId);
		return parent ? `${parent.name} / ${category.name}` : category.name;
	});

	// 等当前模式数据加载完成后再处理失效选择，并用 replace 清除其他模式的参数。
	// 规范化不新增浏览历史，刷新和前进后退时仍由 URL 恢复筛选状态。
	watch(
		() => [ready.value, JSON.stringify(route.query), JSON.stringify(query.value)],
		() => {
			if (!ready.value || !query.value) {
				return;
			}

			const canonicalQuery: Record<string, string> = { mode: mode.value };
			for (const [key, value] of Object.entries(query.value)) {
				canonicalQuery[key] = String(value);
			}

			if (
				Object.keys(route.query).length !== Object.keys(canonicalQuery).length ||
				Object.entries(canonicalQuery).some(([key, value]) => route.query[key] !== value)
			) {
				void router.replace({ name: 'Archive', query: canonicalQuery });
			}
		},
		{ immediate: true },
	);

	const navigationLoading = computed(() => {
		if (mode.value === 'time') {
			return yearsLoading.value;
		}
		if (mode.value === 'category') {
			return categories.loading.value;
		}
		return tags.loading.value;
	});
	const navigationFailed = computed(() => {
		if (mode.value === 'time') {
			return yearsFailed.value;
		}
		if (mode.value === 'category') {
			return categories.loadFailed.value;
		}
		return tags.loadFailed.value;
	});

	function changeMode(value: unknown) {
		if (value !== 'time' && value !== 'category' && value !== 'tag') {
			return;
		}

		// 先更新 URL，查询与导航高亮继续由路由状态驱动。
		void router.push({ name: 'Archive', query: { mode: value } });
	}

	function retryNavigation() {
		if (mode.value === 'time') {
			void loadYears();
		} else if (mode.value === 'category') {
			void categories.getEnabledCategoryList();
		} else {
			void tags.getEnabledTagList();
		}
	}

	return {
		timeZone,
		mode,
		changeMode,
		query,
		title,
		ready,
		years,
		categories: categories.enabledCategoryList,
		tags: tags.enabledTagList,
		navigationLoading,
		navigationFailed,
		retryNavigation,
	};
}
