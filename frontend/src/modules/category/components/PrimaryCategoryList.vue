<template>
	<CategoryListItem
		:category="category"
		v-model:expanded="childrenVisible"
	/>

	<template v-if="childrenVisible">
		<CategoryListItem
			v-for="c in category.children"
			:key="c.id"
			:category="c"
			:border-left="true"
			class="sub-category-list-item"
		/>
	</template>
</template>

<script setup lang="ts">
import { watch } from 'vue';
import { ref } from 'vue';
import type { PublicCategoryItem } from '../types/category.ts';
import CategoryListItem from './CategoryListItem.vue';
import { useRoute } from 'vue-router';

const route = useRoute();

const childrenVisible = ref(false);

defineOptions({
	name: 'PrimaryCategoryListItem',
});

const props = defineProps<{
	category: PublicCategoryItem;
}>();

// 当直接访问 /categories/某个二级分类id 时，父级目录展开
watch(
	() => route.params.categoryId,
	(categoryIdParam) => {
		const categoryIdStr = Array.isArray(categoryIdParam) ? categoryIdParam[0] : categoryIdParam;
		const categoryId = Number(categoryIdStr);
		if (
			categoryIdStr &&
			Number.isInteger(categoryId) &&
			categoryId > 0 &&
			props.category.children.find((child) => child.id === categoryId)
		) {
			childrenVisible.value = true;
		}
	},
	{ immediate: true },
);
</script>

<style lang="scss" scoped>
.sub-category-list-item {
	padding-left: 2rem;
}
</style>
