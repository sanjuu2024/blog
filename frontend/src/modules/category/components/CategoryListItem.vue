<template>
	<!-- data-category-id 用于分类侧栏自动定位中心（跳转分类时） -->
	<div
		class="category-list-item"
		:data-category-id="category.id"
	>
		<div
			class="category-list-item__inner"
			:class="{ 'category-list-item__inner--border-left': borderLeft }"
		>
			<RouterLink
				class="category-list-item__link"
				:class="{ 'category-list-item__link--border-left': borderLeft }"
				:to="{ name: 'CategoryArticlesPage', params: { categoryId: category.id } }"
			>
				<div class="category-list-item__up">
					<h3 class="category-list-item__name">{{ category.name }}</h3>
					<span class="category-list-item__count">{{ category.articleCount }}</span>
				</div>
				<div class="category-list-item__down">
					<h3 class="category-list-item__desc">{{ category.description }}</h3>
				</div>
			</RouterLink>

			<button
				v-if="category.children.length > 0"
				type="button"
				aria-label="展开子分类"
				:aria-expanded="expanded"
				:class="{
					'rotate-180': expanded,
				}"
				class="category-list-item__arrow mr-0.5 cursor-pointer transition-transform duration-100 ease-in-out"
				@click.stop="handleClickArrow"
			>
				<i-lucide-chevron-down />
			</button>
		</div>
	</div>
</template>

<script setup lang="ts">
import type { PublicCategoryItem } from '../types/category';

defineOptions({
	name: 'CategoryListItem',
});

defineProps<{
	category: PublicCategoryItem;
	borderLeft?: boolean;
}>();

const expanded = defineModel<boolean>('expanded', {
	default: false,
});

function handleClickArrow() {
	expanded.value = !expanded.value;
}
</script>

<style lang="scss" scoped>
.category-list-item {
	position: relative;
	display: flex;
	width: 100%;

	.category-list-item__inner {
		display: flex;
		width: 100%;

		&.category-list-item__inner--border-left {
			border-left: 4px solid var(--app-router-hover);
		}

		.category-list-item__arrow {
			position: absolute;
			right: 1rem;
			bottom: 1rem;
		}

		.category-list-item__link {
			width: 100%;
			padding: 1rem;
			border-radius: 0.7rem;
			margin-block: 0.1rem;
			transition:
				background-color 0.2s ease,
				color 0.2s ease;
			color: var(--app-text-muted);

			&:hover {
				background-color: var(--app-router-hover);
			}

			&.router-link-active,
			&.router-link-exact-active {
				background-color: var(--app-router-hover);
				color: var(--app-text);
			}

			&.category-list-item__link--border-left {
				border-radius: 0 0.7rem 0.7rem 0;
			}

			.category-list-item__up {
				display: flex;
				align-items: center;
				justify-content: space-between;
				margin-bottom: 8px;

				.category-list-item__name {
					font-weight: 700;
				}

				.category-list-item__count {
					background: var(--app-surface-muted);
					color: var(--app-text-muted);
					font-weight: 550;
					font-size: 0.8rem;
					border-radius: 9999px;
					padding: 0 0.4rem;
				}
			}

			.category-list-item__down {
				display: flex;
				align-items: center;
				justify-content: space-between;

				.category-list-item__desc {
					font-size: 0.8rem;
					color: var(--app-text-muted);
				}
			}
		}
	}
}
</style>
