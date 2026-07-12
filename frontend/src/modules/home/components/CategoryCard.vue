<template>
	<div class="category-card">
		<div class="number">
			{{ number.toString().padStart(2, '0') }}
		</div>

		<div class="title">
			<RouterLink
				:to="{
					name: 'CategoryArticlesPage',
					params: {
						categoryId: category.id,
					},
				}"
			>
				{{ category.name }}
			</RouterLink>
		</div>

		<div class="desc">
			{{ category.description }}
		</div>

		<div class="children">
			<RouterLink
				v-for="child in category.children?.slice(0, 5) ?? []"
				:key="child.id"
				:to="{
					name: 'CategoryArticlesPage',
					params: {
						categoryId: child.id,
					},
				}"
			>
				{{ child.name }}
			</RouterLink>
			<p v-if="category.children?.length > 5">> ......</p>
		</div>
	</div>
</template>

<script setup lang="ts">
import type { PublicCategoryItem } from '@/modules/category/types/category';

defineOptions({
	name: 'CategoryCard',
});

defineProps<{
	category: PublicCategoryItem;
	number: number;
}>();
</script>

<style lang="scss" scoped>
.category-card {
	border: 2px solid var(--app-border);
	padding: 1rem;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 0.5rem;
	height: 100%;
	transition:
		border 0.1s ease-in-out,
		color 0.1s ease-in-out,
		transform 0.1s ease-in-out;

	&:hover,
	&:focus-within {
		border: 2px solid var(--app-main);
		transform: translateY(-0.2rem);

		.title {
			color: var(--app-main);
		}
	}

	.number {
		font-size: 0.9rem;
		font-weight: 800;
		color: var(--app-main);
	}

	.title {
		font-size: 1.5rem;
		font-weight: bold;
	}

	.desc {
		font-size: 0.9rem;
		color: var(--app-text-muted);

		&::before {
			content: '“';
			font-size: 1.2rem;
		}

		&::after {
			content: '”';
			font-size: 1.2rem;
		}
	}

	.children {
		display: flex;
		flex-direction: column;
		gap: 0.2rem;
		list-style: disc;
		overflow: hidden;

		> a::before {
			content: '>';
			margin-right: 0.5rem;
		}

		> a:hover,
		> a:focus-visible {
			color: var(--app-main);
		}

		.children-has-more {
			::after {
				content: '...';
			}
		}
	}
}
</style>
