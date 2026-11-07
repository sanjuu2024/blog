<template>
	<el-dialog
		:model-value="modelValue"
		fullscreen
		destroy-on-close
		:show-close="false"
		:teleported="false"
		class="admin-article-preview-dialog"
		@update:model-value="emit('update:modelValue', $event)"
	>
		<template #header="{ close }">
			<div
				ref="previewHeader"
				class="admin-article-preview-dialog-header"
			>
				<div class="admin-article-preview-dialog-header__inner">
					<span>实际效果预览</span>
					<el-button
						type="danger"
						@click="close"
					>
						<i-ep-circle-close class="mr-1" />Close
					</el-button>
				</div>
			</div>
		</template>
		<div
			v-if="loading"
			class="preview-state"
			role="status"
		>
			正在生成预览…
		</div>
		<div
			v-else-if="failed"
			class="preview-state"
			role="alert"
		>
			<p>预览加载失败，请重试。</p>
			<el-button @click="emit('retry')">重新预览</el-button>
		</div>
		<article
			v-else-if="article"
			class="article-preview"
		>
			<AppImage
				v-if="article.coverUrl"
				:url="article.coverUrl"
				object-fit="contain"
				alt="文章封面图"
				class="aspect-26/10 w-full rounded-xl"
			/>
			<h1 class="mt-12 mb-4 text-center text-4xl font-bold">
				{{ article.title || '未填写标题' }}
			</h1>
			<p
				v-if="article.categoryPath"
				class="mb-4 text-center text-lg font-bold"
			>
				{{ article.categoryPath }}
			</p>
			<div
				v-if="article.tags.length"
				class="mb-4 flex flex-wrap justify-center gap-2"
			>
				<AppTagCapsule
					v-for="tag in article.tags"
					:key="tag.id"
					:name="tag.name"
				/>
			</div>
			<p
				v-if="article.summary"
				class="preview-summary"
			>
				{{ article.summary }}
			</p>
			<ArticleBody :html="article.contentHtml" />
		</article>

		<AppBacktop
			v-if="scrollTarget"
			:target="scrollTarget"
		/>
	</el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import AppBacktop from '@/components/AppBacktop.vue';
import AppImage from '@/components/AppImage.vue';
import AppTagCapsule from '@/components/AppTagCapsule.vue';
import ArticleBody from '@/modules/article/components/ArticleBody.vue';
import type { ArticlePreviewSnapshot } from '../types/adminArticle';

defineProps<{
	modelValue: boolean;
	loading: boolean;
	failed: boolean;
	article: ArticlePreviewSnapshot | null;
}>();
const emit = defineEmits<{
	'update:modelValue': [value: boolean];
	retry: [];
}>();

const previewHeader = ref<HTMLElement | null>(null);
// 全屏 el-dialog 自身滚动；关闭销毁后引用清空，重新打开时使用新容器。
const scrollTarget = computed(
	() => previewHeader.value?.closest<HTMLElement>('.el-dialog') ?? null,
);
</script>

<style scoped lang="scss">
:global(.el-dialog.admin-article-preview-dialog) {
	background: var(--app-bg);
	color: var(--app-text);
	padding-top: 0;
}

// sticky 放在实际 header 上，避免被插槽外层 header 的高度限制。
:global(.el-dialog.admin-article-preview-dialog > .el-dialog__header) {
	position: sticky;
	top: 0;
	z-index: 10;
	padding-block: 1rem;
	background: var(--app-bg);
}

.admin-article-preview-dialog-header {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 100%;

	.admin-article-preview-dialog-header__inner {
		display: flex;
		align-items: center;
		justify-content: space-between;
		width: calc(min(100%, var(--app-article-detail-width)));
		margin-inline: auto;

		span {
			font-weight: bold;
			font-size: 1.25rem;
		}
	}
}

.article-preview {
	max-width: var(--app-article-detail-width);
	margin-inline: auto;
	padding-block: 2rem;
	color: var(--app-text);
	overflow-wrap: anywhere;
}

.preview-state {
	display: flex;
	min-height: 50vh;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 1rem;
}

.preview-summary {
	margin-block: 1rem;
	text-align: center;
	color: var(--app-text-muted);
	font-size: 0.875rem;

	&::before {
		content: '“';
	}

	&::after {
		content: '”';
	}
}
</style>
