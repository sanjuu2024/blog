<template>
	<AppFixedButton
		class="article-like-button"
		:class="{
			'article-like-button-expanded': expanded,
		}"
		placement="bottom-left"
		:offset-y="props.offsetY"
		aria-label="点赞文章"
		@click="
			(e) => {
				if (!isLiked) triggerConfetti(e, 45);
				emit('toggle');
			}
		"
	>
		<i-lucide-thumbs-up
			class="text-xl"
			:class="{ 'article-like-button--filled': isLiked }"
		/>
	</AppFixedButton>
</template>

<script setup lang="ts">
import AppFixedButton from '@/components/AppFixedButton.vue';
import { useLikeConfetti } from '../composables/useLikeConfetti';
defineOptions({
	name: 'ArticleLikeButton',
});

const { triggerConfetti } = useLikeConfetti();

const props = defineProps<{
	offsetY: number;
}>();

const emit = defineEmits<{ toggle: [] }>();

const expanded = defineModel<boolean>('expanded', {
	required: true,
});

// 当前用户是否已点赞
const isLiked = defineModel<boolean>('isLiked', {
	required: true,
});
</script>

<style scoped lang="scss">
// 侧栏拉开/关上的按钮
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
