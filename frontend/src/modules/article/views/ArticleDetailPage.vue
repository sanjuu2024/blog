<template>
	<div class="article-detail">
		<ArticleContent
			:article="article"
			:is-loading="isLoading"
			:error-message="errorMessage"
		/>

		<AppBacktop />
	</div>
</template>

<script setup lang="ts">
import { watch } from 'vue';
import ArticleContent from '../components/ArticleContent.vue';
import { useArticleDetail } from '../composables/useArticleDetail';
import { useRoute } from 'vue-router';

const route = useRoute();

const { article, isLoading, errorMessage, getArticleDetail } = useArticleDetail();

defineOptions({
	name: 'ArticleDetail',
});

// 同路由组件、参数变化情况下，需要重新获取文章详情
// 例如：从 /article/1 切换到 /article/2，组件实例不变，但需要重新获取文章详情数据
// 既然有了 watch（而且主要开了 immediate: true），就不需要 onMounted 重复调用了
watch(
	() => route.params.articleId,
	(articleId) => {
		getArticleDetail(articleId);
	},
	{ immediate: true },
);
</script>

<style></style>
