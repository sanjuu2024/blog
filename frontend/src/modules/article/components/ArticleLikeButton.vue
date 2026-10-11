<template>
	<button
		class="article-like-button"
		:class="{
			'article-like-button-expanded': expanded,
			'article-like-button--no-catelog': !hasCatalog,
		}"
		aria-label="点赞文章"
		@click="
			(e) => {
				triggerConfetti(e, 45);
				emit('toggle');
			}
		"
	>
		<i-lucide-thumbs-up
			class="text-xl"
			:class="{ 'article-like-button--filled': isLiked }"
		/>
	</button>
</template>

<script setup lang="ts">
import { useLikeConfetti } from '../composables/useLikeConfetti';
defineOptions({
	name: 'ArticleLikeButton',
});

const { triggerConfetti } = useLikeConfetti();

const emit = defineEmits<{ toggle: [] }>();

const expanded = defineModel<boolean>('expanded', {
	required: true,
});

const hasCatalog = defineModel<boolean>('hasCatalog', {
	required: true,
});

// 当前用户是否已点赞
const isLiked = defineModel<boolean>('isLiked', {
	required: true,
});
</script>

<style scoped lang="scss">
// 侧栏拉开/关上的按钮
.article-like-button {
	position: fixed;
	left: 32px;
	bottom: 100px;
	width: 44px;
	height: 44px;
	border-radius: 9999px;
	background-color: var(--app-surface);
	color: var(--app-text-muted);
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 0 4px rgb(0 0 0 / 20%);
	transition:
		transform 0.2s ease,
		background-color 0.1s ease;

	&:hover {
		color: var(--app-main);
	}

	&:focus-visible {
		outline: 2px solid var(--app-main);
		outline-offset: 2px;
	}

	z-index: 999;
	cursor: pointer;
}

.article-like-button--no-catelog {
	bottom: 32px;
}

.article-like-button-expanded {
	transform: translateX(calc(var(--app-article-catalog-sidebar-width) - 16px));
}

:deep(.article-like-button--filled) {
	color: var(--app-main);

	// 作用于svg 中的 path 元素，才能实行真的填充
	// （废案，lucide 图标确实不适合填充，点赞图标填充后作为袖口的线也会没掉，虽然可以恢复 stroke并且用黑色但是不好看，想用白色 stroke 但是是背景色，图标大小就像缩水了一样（
	// 因此已点赞就直接修改描边颜色即可吧。
	// * {
	//     fill: var(--app-main);
	//     stroke: var(--app-text);   // 黑色不好看，白色同背景色，不用 stroke 又不行
	//     color: var(--app-surface);
	// }
}
</style>
