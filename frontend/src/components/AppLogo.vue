<template>
	<component
		:is="logoComponent"
		:to="to"
		class="app-logo"
		:class="{ 'is-icon-only': !showText }"
		:aria-label="ariaLabel"
		:style="{ '--app-logo-size': logoSize }"
	>
		<img
			class="app-logo__mark"
			src="/favicon.svg"
			:alt="showText ? '' : alt"
			:aria-hidden="showText"
			:style="{ width: logoSize, height: logoSize }"
		/>
		<span
			v-if="showText"
			class="app-logo__text"
			>{{ text }}</span
		>
	</component>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, type RouteLocationRaw } from 'vue-router';

const props = withDefaults(
	defineProps<{
		size?: number | string;
		text?: string;
		showText?: boolean;
		alt?: string;
		to?: RouteLocationRaw;
	}>(),
	{
		size: 32,
		text: import.meta.env.VITE_APP_TITLE || 'Sanjuu Blog',
		showText: true,
		alt: '网站 Logo',
		to: undefined,
	},
);

// 数字尺寸转成 CSS 可用的 px；字符串尺寸保留原样，例如 '2rem'。
const logoSize = computed(() => (typeof props.size === 'number' ? `${props.size}px` : props.size));

// 传入 to 时渲染成 RouterLink，可以点击跳转；不传 to 时渲染成普通 span。
const logoComponent = computed(() => (props.to ? RouterLink : 'span'));

// 组合 Logo 读网站名；只显示图标时读 alt，方便屏幕阅读器理解。
const ariaLabel = computed(() => (props.showText ? props.text : props.alt));
</script>

<style scoped lang="scss">
.app-logo {
	display: inline-flex;
	align-items: center;
	gap: 0.55rem;
	color: var(--app-text);
	text-decoration: none;
	vertical-align: middle;
}

.app-logo.is-icon-only {
	gap: 0;
}

.app-logo__mark {
	display: block;
	flex: none;
	object-fit: contain;
}

.app-logo__text {
	color: inherit;
	font-size: var(--app-logo-size);
	font-weight: 500;
	line-height: 1.1;
	letter-spacing: 0;
	white-space: nowrap;
}
</style>
