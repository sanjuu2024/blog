<template>
	<button
		class="app-fixed-button"
		:class="`app-fixed-button--${props.placement}`"
		:style="{
			'--app-fixed-button-offset-x': `${props.offsetX}px`,
			'--app-fixed-button-offset-y': `${props.offsetY}px`,
		}"
		type="button"
		@click="emit('click', $event)"
	>
		<slot></slot>
	</button>
</template>

<script setup lang="ts">
defineOptions({
	name: 'AppFixedButton',
});

const props = withDefaults(
	defineProps<{
		placement?: 'bottom-left' | 'bottom-right';
		offsetX?: number;
		offsetY?: number;
	}>(),
	{
		placement: 'bottom-right',
		offsetX: 32,
		offsetY: 32,
	},
);

const emit = defineEmits<{
	click: [event: MouseEvent];
}>();
</script>

<style scoped lang="scss">
.app-fixed-button {
	position: fixed;
	bottom: var(--app-fixed-button-offset-y);
	width: 44px;
	height: 44px;
	border-radius: 9999px;
	background-color: var(--app-surface);
	color: var(--app-text-muted);
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 0 6px color-mix(in srgb, var(--app-text) 35%, transparent);
	transition:
		transform 0.2s ease,
		background-color 0.1s ease;

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

.app-fixed-button--bottom-left {
	left: var(--app-fixed-button-offset-x);
}

.app-fixed-button--bottom-right {
	right: var(--app-fixed-button-offset-x);
}
</style>
