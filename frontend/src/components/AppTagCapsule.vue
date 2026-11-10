<template>
	<span
		class="inline-flex shrink-0 items-center rounded-full bg-(--app-tag-capsule-bg-color) px-2 py-0.5 text-xs whitespace-nowrap text-(--app-tag-capsule-text-color)"
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
