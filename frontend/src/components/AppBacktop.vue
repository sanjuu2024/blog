<template>
	<AppFixedButton
		class="app-backtop__button"
		aria-label="回到顶部"
		@click="scrollToTop"
	>
		<i-lucide-arrow-up class="text-xl" />
	</AppFixedButton>
</template>

<script setup lang="ts">
import AppFixedButton from './AppFixedButton.vue';

defineOptions({
	name: 'AppBacktop',
});

const props = defineProps<{
	target?: HTMLElement | null;
}>();

function scrollToTop() {
	(props.target ?? window).scrollTo({
		top: 0,
		behavior: 'smooth',
	});
}
</script>
