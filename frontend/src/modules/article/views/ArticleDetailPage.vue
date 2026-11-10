<template>
	<div
		ref="pageRef"
		class="article-detail"
	>
		<ArticleContent
			:article="article"
			:is-loading="isLoading"
			:error-message="errorMessage"
			:updating-like="updatingLike"
			@toggle-like="toggleArticleLike"
			@jump-to-comments="jumpToComments"
		/>

		<PublicCommentSection
			v-if="article"
			:article="article"
			@comment-count-change="changeCommentCount"
			@reply-located="locateReply"
		/>

		<AppBacktop />
	</div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';
import ArticleContent from '../components/ArticleContent.vue';
import PublicCommentSection from '@/modules/comment/components/PublicCommentSection.vue';
import { useArticleDetail } from '../composables/useArticleDetail';
import { useRoute } from 'vue-router';
import { useArticleCommentAnchor } from '../composables/useArticleCommentAnchor';

const route = useRoute();
const pageRef = ref<HTMLElement | null>(null);
const { locate, stop } = useArticleCommentAnchor(pageRef);
let manualCommentNavigation = false;

const { article, isLoading, errorMessage, updatingLike, getArticleDetail, toggleArticleLike } =
	useArticleDetail();

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
	[() => route.fullPath, () => article.value?.id],
	async (_, __, onCleanup) => {
		stop();
		manualCommentNavigation = false;
		let cancelled = false;
		onCleanup(() => {
			cancelled = true;
			stop();
		});
		// 回复通知由评论区报告实际目标，避免先滚动到开头再抢走回复定位。
		if (route.hash !== '#article-comments' || !article.value || route.query.replyId) return;
		await nextTick();
		if (cancelled) return;
		const comments = pageRef.value?.querySelector<HTMLElement>('#article-comments');
		if (comments) locate(comments);
	},
	{ flush: 'post' },
);

function jumpToComments() {
	manualCommentNavigation = true;
	const comments = pageRef.value?.querySelector<HTMLElement>('#article-comments');
	if (comments) locate(comments, 'smooth');
}

function locateReply(target: HTMLElement) {
	// 通知回复可能晚于用户点击才加载完成，此时不能再抢回滚动位置。
	if (!manualCommentNavigation) locate(target);
}

// 评论创建或删除后，直接更新本地统计，避免重新拉取文章详情导致页面滚动回顶部
function changeCommentCount(delta: number) {
	if (!article.value) return;

	article.value.commentCount = Math.max(0, article.value.commentCount + delta);
}
</script>
