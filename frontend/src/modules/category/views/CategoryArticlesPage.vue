<template>
	<ArticleListItem
		v-for="article in articles"
		:key="article.id"
		:article="article"
	/>
	<AppLoadMoreTrigger
		:loading="loading"
		:has-next="pageParams.hasNext"
		thing-str="文章"
		@load-more="loadMoreArticles"
	/>
</template>

<script setup lang="ts">
import { watch } from 'vue';
import ArticleListItem from '@/modules/article/components/ArticleListItem.vue';
import AppLoadMoreTrigger from '@/components/AppLoadMoreTrigger.vue';
import { useArticleList } from '@/modules/article/composables/useArticleList';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();

defineOptions({
	name: 'CategoryArticlesPage',
});

const {
	articles,
	pageParams,
	filterForm,
	loading,
	loadMoreArticles,
	resetPageParamsAndGetPublishedArticles,
} = useArticleList();

watch(
	() => route.params.categoryId,
	(categoryIdParam) => {
		const categoryIdStr = Array.isArray(categoryIdParam) ? categoryIdParam[0] : categoryIdParam;

		if (!categoryIdStr) {
			filterForm.categoryId = undefined;
			resetPageParamsAndGetPublishedArticles();
			return;
		}

		const categoryId = Number(categoryIdStr);
		if (!Number.isInteger(categoryId) || categoryId <= 0) {
			ElMessage.error('分类 ID 无效:' + categoryIdStr);
			router.replace({ name: 'CategoryAllArticles' });
			return;
		}

		filterForm.categoryId = categoryId;
		resetPageParamsAndGetPublishedArticles();
	},
	{ immediate: true },
);
</script>

<style></style>
