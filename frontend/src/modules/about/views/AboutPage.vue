<template>
	<div class="about-page">
		<AppLoading :loading="loading">
			<div
				v-if="loadFailed"
				class="about-page__state"
			>
				<p>关于页加载失败</p>
				<el-button
					type="primary"
					@click="getAboutPageContent"
				>
					重试
				</el-button>
			</div>
			<div
				v-else-if="aboutPage"
				ref="contentRef"
				class="about-content article-markdown markdown-body"
				v-html="aboutPage.contentHtml"
			></div>
		</AppLoading>
	</div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue';
import { useAboutPage } from '../composables/useAboutPage';
import { renderImageCaptions } from '@/modules/article/utils/renderImageCaptions';

defineOptions({
	name: 'AboutPage',
});

const { aboutPage, loading, loadFailed, getAboutPageContent } = useAboutPage();
const contentRef = ref<HTMLElement | null>(null);

watch(
	() => aboutPage.value?.contentHtml,
	async () => {
		await nextTick();
		// 复用文章图注规则，仅处理渲染后的 DOM，保留后端正文中的 title。
		if (contentRef.value) renderImageCaptions(contentRef.value);
	},
	{ immediate: true },
);

onMounted(() => {
	getAboutPageContent();
});
</script>

<style lang="scss" scoped>
.about-page {
	margin: 0 auto;
	padding-bottom: 3rem;
	max-width: var(--app-about-content-width);
}

.about-content {
	padding: 0;

	// :deep(p) {
	// 	text-indent: 2em;
	// }

	:deep(a) {
		color: var(--app-main);
		text-decoration: underline;
		font-weight: bold;
	}
}

.about-page__state {
	display: flex;
	min-height: 10rem;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 1rem;
	color: var(--app-text-muted);
}
</style>
