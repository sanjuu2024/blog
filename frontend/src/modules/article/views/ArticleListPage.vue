<template>
	<div class="article-list-page">
		<!-- 搜索栏单独吸顶，分类和标签筛选会随页面内容正常滚动 -->
		<div class="article-search-sticky">
			<form
				class="article-search-form"
				@submit.prevent="applyFilterToRoute"
			>
				<el-input
					v-model="filterForm.keyword"
					maxlength="100"
					clearable
					placeholder="搜索文章标题、摘要或正文"
					aria-label="搜索文章"
					@clear="applyFilterToRoute"
				/>
				<el-button
					class="article-search-reset-button"
					aria-label="重置文章筛选条件"
					native-type="button"
					@click="resetFilterAndRoute"
				>
					<i-lucide-rotate-ccw />
					<span>重置</span>
				</el-button>
				<el-button
					class="article-search-button"
					aria-label="搜索文章"
					native-type="submit"
				>
					<i-lets-icons-search-alt />
					<span>搜索</span>
				</el-button>
			</form>
		</div>

		<!-- 分类和标签使用紧凑筛选工具栏，并随页面内容正常滚动 -->
		<ArticleFilterPanel
			v-model:filter-form="filterForm"
			class="article-filter-panel"
			:category-list="enabledCategoryList"
			:tag-list="enabledTagList"
		/>

		<section class="article-result-area">
			<p
				v-if="activeKeyword && hasLoaded"
				class="article-result-summary"
			>
				共找到 {{ total }} 篇与“{{ activeKeyword }}”相关的文章
			</p>

			<div
				class="article-list divide-y divide-(--app-border) rounded bg-(--app-surface) px-8"
			>
				<!-- 文章列表 -->
				<ArticleListItem
					v-for="article in articles"
					:key="article.id"
					:article="article"
					class="article-list-item"
				/>

				<p
					v-if="hasLoaded && articles.length === 0"
					class="article-empty-result"
				>
					{{ activeKeyword ? '没有找到相关文章' : '暂无符合筛选条件的文章' }}
				</p>

				<!-- 滚动加载更多的触发器 -->
				<AppLoadMoreTrigger
					v-if="!hasLoaded || articles.length > 0"
					:loading="loading"
					:has-next="pageParams.hasNext"
					:thing-str="'文章'"
					root-margin="0px"
					@load-more="loadMoreArticles"
				/>
			</div>
		</section>

		<AppBacktop />
		<!-- 回到顶部按钮 -->
	</div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ArticleListItem from '../components/ArticleListItem.vue';
import AppLoadMoreTrigger from '@/components/AppLoadMoreTrigger.vue';
import AppBacktop from '@/components/AppBacktop.vue';
import ArticleFilterPanel from '../components/ArticleFilterPanel.vue';
import { useArticleList } from '../composables/useArticleList';
import { useEnabledCategoryList } from '@/modules/category/composables/useEnabledCategoryList';
import { useEnabledTagList } from '@/modules/tag/composables/useEnabledTagList';

const {
	articles,
	total,
	hasLoaded,
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
const activeKeyword = computed(() => parseKeyword(route.query.keyword));

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

function parseKeyword(value: unknown): string {
	const rawValue = Array.isArray(value) ? value[0] : value;
	return typeof rawValue === 'string' ? rawValue.trim() : '';
}

function syncFilterFormFromRoute() {
	filterForm.keyword = parseKeyword(route.query.keyword);
	filterForm.categoryId = parsePositiveInteger(route.query.categoryId);
	filterForm.tagIds = parseTagIds(route.query.tagIds);
}

function buildArticleListRouteQuery() {
	const query = { ...route.query };
	const tagIds = filterForm.tagIds ?? [];
	delete query.keyword;
	delete query.categoryId;
	delete query.tagIds;

	const keyword = filterForm.keyword.trim();
	if (keyword) {
		query.keyword = keyword;
	}
	if (filterForm.categoryId !== undefined) {
		query.categoryId = String(filterForm.categoryId);
	}
	if (tagIds.length > 0) {
		query.tagIds = tagIds.map(String);
	}

	return query;
}

async function applyFilterToRoute() {
	await router.push({
		name: 'ArticleList',
		query: buildArticleListRouteQuery(),
	});
}

async function resetFilterAndRoute() {
	resetFilterForm();
	await applyFilterToRoute();
}

watch(
	() => [route.query.keyword, route.query.categoryId, route.query.tagIds],
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
.article-search-sticky {
	position: sticky;
	top: var(--app-header-height);
	z-index: calc(var(--app-header-z-index) - 1);
	padding-block: 1.5rem;
	background-color: var(--app-bg);
}

.article-search-form {
	display: flex;
	gap: 0.75rem;
	width: 100%;
}

.article-search-button {
	--el-button-bg-color: var(--app-button-bg);
	--el-button-border-color: var(--app-button-bg);
	--el-button-text-color: var(--app-button-text);
	--el-button-hover-bg-color: var(--app-button-hover);
	--el-button-hover-border-color: var(--app-button-hover);
	--el-button-hover-text-color: var(--app-button-text);
	--el-button-active-bg-color: var(--app-button-active);
	--el-button-active-border-color: var(--app-button-active);
	--el-button-active-text-color: var(--app-button-text);

	flex: 0 0 auto;
}

.article-search-reset-button {
	flex: 0 0 auto;
}

.article-search-reset-button :deep(> span),
.article-search-button :deep(> span) {
	gap: 0.375rem;
}

.article-filter-panel {
	margin-bottom: 1rem;
}

.article-result-summary {
	margin-bottom: 0.75rem;
	color: var(--app-text-muted);
}

.article-empty-result {
	padding-block: 3rem;
	text-align: center;
	color: var(--app-text-muted);
}

.article-list-item {
	border-bottom: 1px solid var(--app-border);

	&:last-child {
		border-bottom: none;
	}
}

@media (max-width: 767px) {
	.article-search-form {
		gap: 0.5rem;
	}

	.article-search-reset-button span,
	.article-search-button span {
		display: none;
	}
}
</style>
