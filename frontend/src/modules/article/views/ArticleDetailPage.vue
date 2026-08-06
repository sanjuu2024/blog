<template>
	<div class="article-detail">
		<ArticleContent
			:article="article"
			:is-loading="isLoading"
			:error-message="errorMessage"
		/>

		<PublicCommentSection
			v-if="article"
			:article="article"
			@comment-count-change="changeCommentCount"
		/>

		<AppBacktop />
	</div>
</template>

<script setup lang="ts">
import { watch } from 'vue';
import ArticleContent from '../components/ArticleContent.vue';
import PublicCommentSection from '@/modules/comment/components/PublicCommentSection.vue';
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

watch(
	() => article.value?.title,
	(title) => {
		if (title) {
			document.title = `${title} - ${import.meta.env.VITE_APP_TITLE}`;
		}
	},
);

// 评论创建或删除后，直接更新本地统计，避免重新拉取文章详情导致页面滚动回顶部
function changeCommentCount(delta: number) {
	if (!article.value) return;

	article.value.commentCount = Math.max(0, article.value.commentCount + delta);
}
</script>

<style></style>
