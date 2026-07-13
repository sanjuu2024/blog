<template>
	<div class="article-list-page">
		<div class="article-list divide-y divide-(--app-border) rounded bg-(--app-surface) px-8">
			<!-- 文章列表 -->
			<ArticleListItem
				v-for="article in articles"
				:key="article.id"
				:article="article"
				class="article-list-item"
			/>

			<!-- 滚动加载更多的触发器 -->
			<AppLoadMoreTrigger
				:loading="loading"
				:has-next="pageParams.hasNext"
				:thing-str="'文章'"
				root-margin="0px"
				@load-more="loadMoreArticles"
			/>
		</div>

		<AppBacktop />
		<!-- 回到顶部按钮 -->

		<!-- 文章筛选条件侧边栏 -->
		<ArticleFilterSidebar
			v-model:filter-form="filterForm"
			:category-list="enabledCategoryList"
			:tag-list="enabledTagList"
			@reset-filter-form="resetFilterAndRoute"
			@get-list="applyFilterToRoute"
		/>
	</div>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ArticleListItem from '../components/ArticleListItem.vue';
import AppLoadMoreTrigger from '@/components/AppLoadMoreTrigger.vue';
import AppBacktop from '@/components/AppBacktop.vue';
import ArticleFilterSidebar from '../components/ArticleFilterSidebar.vue';
import { useArticleList } from '../composables/useArticleList';
import { useEnabledCategoryList } from '@/modules/category/composables/useEnabledCategoryList';
import { useEnabledTagList } from '@/modules/tag/composables/useEnabledTagList';

const {
	articles,
	pageParams,
	filterForm,
	loading,
	resetFilterForm,
	loadMoreArticles,
	resetPageParamsAndGetPublishedArticles,
} = useArticleList();

const { enabledCategoryList, getEnabledCategoryList } = useEnabledCategoryList();

const { enabledTagList, getEnabledTagList } = useEnabledTagList();

const route = useRoute();
const router = useRouter();

defineOptions({
	name: 'ArticleListPage',
});

function parsePositiveInteger(value: unknown): number | undefined {
	const rawValue = Array.isArray(value) ? value[0] : value;
	const parsedValue = Number(rawValue);

	return Number.isInteger(parsedValue) && parsedValue > 0 ? parsedValue : undefined;
}

function parseTagIds(value: unknown): number[] {
	const values = Array.isArray(value) ? value : [value];
	return values
		.map((item) => parsePositiveInteger(item))
		.filter((item): item is number => item !== undefined);
}

function syncFilterFormFromRoute() {
	filterForm.categoryId = parsePositiveInteger(route.query.categoryId);
	filterForm.tagIds = parseTagIds(route.query.tagIds);
}

function buildArticleListRouteQuery() {
	const query = { ...route.query };
	const tagIds = filterForm.tagIds ?? [];
	delete query.categoryId;
	delete query.tagIds;

	if (filterForm.categoryId !== undefined) {
		query.categoryId = String(filterForm.categoryId);
	}
	if (tagIds.length > 0) {
		query.tagIds = tagIds.map(String);
	}

	return query;
}

function applyFilterToRoute() {
	router.push({
		name: 'ArticleList',
		query: buildArticleListRouteQuery(),
	});
}

function resetFilterAndRoute() {
	resetFilterForm();
	router.push({
		name: 'ArticleList',
		query: buildArticleListRouteQuery(),
	});
}

watch(
	() => [route.query.categoryId, route.query.tagIds],
	async () => {
		syncFilterFormFromRoute();
		await resetPageParamsAndGetPublishedArticles();
	},
	{ immediate: true },
);

onMounted(() => {
	getEnabledCategoryList();
	getEnabledTagList();
});
</script>

<style lang="scss" scoped>
.article-list-item {
	border-bottom: 1px solid var(--app-border);

	&:last-child {
		border-bottom: none;
	}
}
</style>
