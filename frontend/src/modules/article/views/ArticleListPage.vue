<template>
	<div class="article-list-page">
		<div class="article-list divide-y divide-(--app-border) rounded bg-(--app-surface) px-8">
			<!-- 文章列表 -->
			<ArticleListItem
				v-for="article in articles"
				:key="article.id"
				:article="article"
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
			@reset-filter-form="resetFilterForm"
			@get-list="resetPageParamsAndGetPublishedArticles"
		/>
	</div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
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
	getPublishedArticles,
	loadMoreArticles,
	resetPageParamsAndGetPublishedArticles,
} = useArticleList();

const { enabledCategoryList, getEnabledCategoryList } = useEnabledCategoryList();

const { enabledTagList, getEnabledTagList } = useEnabledTagList();

defineOptions({
	name: 'ArticleListPage',
});

onMounted(() => {
	getPublishedArticles();
	getEnabledCategoryList();
	getEnabledTagList();
});
</script>

<style></style>
