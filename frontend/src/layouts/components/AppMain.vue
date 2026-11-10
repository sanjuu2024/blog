<template>
	<main
		class="app-main"
		:class="{ 'app-main--full-height': isFullHeightPage }"
	>
		<div
			class="app-main__inner"
			:class="{ 'app-main__inner--full-height': isFullHeightPage }"
		>
			<RouterView v-slot="{ Component }">
				<transition
					name="fade"
					mode="out-in"
					@before-leave="setPageViewPending(true)"
					@enter="setPageViewPending(false)"
					@enter-cancelled="setPageViewPending(false)"
					@leave-cancelled="setPageViewPending(false)"
				>
					<KeepAlive
						include="ArticleListPage"
						:max="1"
					>
						<component :is="Component" />
					</KeepAlive>
				</transition>
			</RouterView>
		</div>
	</main>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { setPageViewPending } from '@/router/articleListScroll';

const route = useRoute();
const isFullHeightPage = computed(() =>
	route.matched.some((record) => record.meta.fullHeight === true),
);

onMounted(() => setPageViewPending(false));
onUnmounted(() => setPageViewPending(false));

defineOptions({
	name: 'AppMain',
});
</script>

<style lang="scss" scoped>
.app-main {
	flex: 1 0 auto;
	min-height: 0;
	padding-block: var(--app-main-padding-y);
	padding-bottom: 3rem;
}

.app-main__inner {
	width: min(100% - var(--app-page-padding-x) * 2, var(--app-container-width));
	margin-inline: auto;
}

.app-main--full-height {
	flex: 0 0 calc(100dvh - var(--app-header-height));
	height: calc(100dvh - var(--app-header-height));
	min-height: 0;
}

.app-main__inner--full-height {
	height: 100%;
	min-height: 0;
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
