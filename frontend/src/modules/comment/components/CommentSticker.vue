<template>
	<span v-if="failed">[{{ name }}]</span>
	<img
		v-else
		class="comment-sticker"
		:src="url"
		:alt="`[${name}]`"
		:title="name"
		loading="lazy"
		decoding="async"
		@error="failed = true"
	/>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

const props = defineProps<{ name: string; url: string }>();
const failed = ref(false);
watch(
	() => props.url,
	() => (failed.value = false),
);
</script>

<style scoped lang="scss">
.comment-sticker {
	display: inline-block;
	width: auto;
	max-width: none;
	height: 1.5rem;
	vertical-align: text-bottom;
}
</style>
