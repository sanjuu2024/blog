<template>
	<div class="app-theme-switcher">
		<div
			v-if="showTitle"
			class="app-theme-switcher__header mb-2"
		>
			<p class="app-theme-switcher__title">主题设置</p>
			<p class="app-theme-switcher__description">
				当前生效：{{ resolvedTheme === 'dark' ? '深色' : '浅色' }}
			</p>
		</div>
		<div class="app-theme-switcher__options">
			<button
				v-for="theme in themeOptions"
				:key="theme.name"
				type="button"
				class="app-theme-switcher__option"
				:class="{ 'app-theme-switcher__option--active': currentTheme === theme.name }"
				:aria-pressed="currentTheme === theme.name"
				@click.stop="setTheme(theme.name)"
			>
				<span
					class="app-theme-switcher__swatch"
					:style="{ background: theme.swatch }"
				></span>
				<span>{{ theme.label }}</span>
			</button>
		</div>
	</div>
</template>

<script setup lang="ts">
import { useTheme } from '@/composables/useTheme';

interface Props {
	showTitle?: boolean;
}

withDefaults(defineProps<Props>(), {
	showTitle: true,
});

const { currentTheme, resolvedTheme, setTheme, themeOptions } = useTheme();
</script>

<style lang="scss" scoped>
.app-theme-switcher {
	min-width: 250px;
	font-size: 0.8rem;

	.app-theme-switcher__title {
		font-weight: bold;
		margin-bottom: 0.5rem;
		font-size: 1rem;
	}

	.app-theme-switcher__description {
		color: var(--app-text-muted);
		margin-bottom: 1rem;
	}
}

.app-theme-switcher__options {
	display: flex;
	flex-direction: column;
	gap: 1rem;
}

.app-theme-switcher__option {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	width: 100%;
	padding: 0.5rem 0.7rem;
	border: 1px solid var(--app-border);
	border-radius: 0.8rem;
	background: transparent;
	color: var(--app-text);
	cursor: pointer;
}

.app-theme-switcher__option:hover,
.app-theme-switcher__option--active {
	border-color: var(--app-button-bg);
	color: var(--app-button-bg);
	font-weight: bold;
}

.app-theme-switcher__swatch {
	width: 1rem;
	height: 1rem;
	border: 2px solid var(--app-border);
	border-radius: 999px;
}
</style>
