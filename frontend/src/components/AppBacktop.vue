<template>
	<button
		class="app-backtop__button"
		aria-label="回到顶部"
		@click="scrollToTop"
	>
		<i-lucide-arrow-up class="text-xl" />
	</button>
</template>

<script setup lang="ts">
defineOptions({
	name: 'AppBacktop',
});

function scrollToTop() {
	const scrollContainer = document.querySelector<HTMLElement>('.app-layout');

	scrollContainer?.scrollTo({
		top: 0,
		behavior: 'smooth',
	});
}
</script>

<style scoped lang="scss">
.app-backtop__button {
	position: fixed;
	right: 32px;
	bottom: 32px;
	width: 44px;
	height: 44px;
	border-radius: 9999px;
	background-color: var(--app-surface);
	color: var(--app-text-muted);
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 0 4px rgb(0 0 0 / 20%);
	transition: background-color 0.1s ease;

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
</style>
