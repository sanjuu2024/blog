<template>
	<a
		:href="friendLink.url"
		target="_blank"
		rel="noopener noreferrer"
		class="friend-link-item"
	>
		<div
			v-if="!friendLink.logoUrl"
			class="friend-link-item__logo-fallback"
		>
			<i-streamline-ultimate-color-network-browser />
		</div>
		<AppImage
			v-else
			:url="friendLink.logoUrl"
			:alt="friendLink.name"
			:rounded="false"
			class="friend-link-item__logo"
		/>
		<div class="friend-link-item__content">
			<h3 :title="friendLink.name">{{ friendLink.name }}</h3>
			<p v-if="friendLink.description">{{ friendLink.description }}</p>
		</div>
	</a>
</template>

<script setup lang="ts">
import AppImage from '@/components/AppImage.vue';
import type { PublicFriendLinkItem } from '../types/friendLink';

defineOptions({ name: 'FriendLinkItem' });

defineProps<{
	friendLink: PublicFriendLinkItem;
}>();
</script>

<style scoped lang="scss">
.friend-link-item {
	display: flex;
	min-width: 0;
	flex-direction: column;
	border: 1px solid var(--app-border);
	background: var(--app-bg);
	transition:
		border 0.15s ease,
		transform 0.1s ease-in-out;

	&:hover,
	&:focus-visible {
		outline: 2px solid var(--app-main);
		transform: translateY(-0.2rem);
	}
}

.friend-link-item__logo,
.friend-link-item__logo-fallback {
	width: 100%;
	height: auto;
	aspect-ratio: 1;
	flex: 0 0 auto;
	border-bottom: 1px solid var(--app-border);
	background-color: var(--app-default-img-bg-color);
}

.friend-link-item__logo-fallback {
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 2.5rem;
}

.friend-link-item__content {
	display: flex;
	min-height: 3rem;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	min-width: 0;
	width: 100%;
	overflow: hidden;
	padding: 0.75rem 1rem;
	text-align: center;

	h3 {
		display: -webkit-box;
		width: 100%;
		overflow: hidden;
		margin-bottom: 0.25rem;
		font-size: 1.25rem;
		font-weight: bold;
		-webkit-box-orient: vertical;
		line-clamp: 2;
		-webkit-line-clamp: 2;
	}

	p {
		display: -webkit-box;
		width: 100%;
		overflow: hidden;
		margin-block: 0.35rem;
		font-size: 0.85rem;
		-webkit-box-orient: vertical;
		line-clamp: 3;
		-webkit-line-clamp: 3;
	}
}
</style>
