<template>
	<main class="app-main">
		<div class="app-main__inner">
			<RouterView v-slot="{ Component }">
				<transition
					name="fade"
					mode="out-in"
				>
					<component :is="Component" />
				</transition>
			</RouterView>
		</div>
	</main>
</template>

<script setup lang="ts">
defineOptions({
	name: 'AppMain',
});
</script>

<style lang="scss" scoped>
.app-main {
	height: calc(100dvh - var(--app-header-height));
	padding-block: var(--app-main-padding-y);
}

.app-main__inner {
	width: min(100% - var(--app-page-padding-x) * 2, var(--app-container-width));
	height: 100%;
	margin-inline: auto;
}

.fade-enter-from,
.fade-leave-to {
	opacity: 0;
	transform: translateY(0.25rem);
}

.fade-enter-active,
.fade-leave-active {
	transition:
		opacity 0.2s ease,
		transform 0.2s ease;
}

.fade-enter-to,
.fade-leave-from {
	opacity: 1;
	transform: translateY(0);
}
</style>
