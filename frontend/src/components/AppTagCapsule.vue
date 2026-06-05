<template>
	<span
		class="inline-flex shrink-0 items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs whitespace-nowrap text-gray-500"
	>
		#{{ name }}
	</span>
</template>

<script setup lang="ts">
defineOptions({
	name: 'AppTagCapsule',
});

defineProps<{
	name: string;
}>();
</script>

<style></style>
