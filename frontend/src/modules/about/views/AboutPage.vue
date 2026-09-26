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
				class="about-content article-markdown markdown-body"
				v-html="aboutPage.contentHtml"
			></div>
		</AppLoading>
	</div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useAboutPage } from '../composables/useAboutPage';

defineOptions({
	name: 'AboutPage',
});

const { aboutPage, loading, loadFailed, getAboutPageContent } = useAboutPage();

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
	line-height: 2rem;

	// :deep(p) {
	// 	text-indent: 2em;
	// }
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
