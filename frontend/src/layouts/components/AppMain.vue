<template>
	<main class="app-main">
		<RouterView v-slot="{ Component }">
			<transition name="fade">
				<component :is="Component" />
			</transition>
		</RouterView>
	</main>
</template>

<script setup lang="ts">
defineOptions({
	name: 'AppMain',
});
</script>

<style lang="scss" scoped>
.app-main {
	min-height: calc(100dvh - var(--app-header-height));
	padding-block: var(--app-main-padding-y);
}

.fade-enter-from {
	opacity: 0;
	transform: scale(0);
}

.fade-enter-active {
	transition: all 0.5s ease;
}

.fade-enter-to {
	opacity: 1;
	transform: scale(1);
}
</style>
