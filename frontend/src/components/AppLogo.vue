<template>
	<!-- 如果传了 to，它会渲染成 RouterLink，Logo 可以点击跳转。
		如果没传 to，它会渲染成普通 span。 -->
	<!-- 当 showText 为 false 时，只显示图标（!showText = true，则有 'is-icon-only' 类） -->
	<!-- aria-hidden="true" 表示这个元素对屏幕阅读器隐藏。这里文字 SVG 只是视觉图形，所以隐藏它，让外层统一读 aria-label。 -->
	<component
		:is="logoComponent"
		:to="to"
		class="app-logo"
		:class="{ 'is-icon-only': !showText }"
		:aria-label="ariaLabel"
		:style="logoStyle"
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
			aria-hidden="true"
		></span>
	</component>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, type RouteLocationRaw } from 'vue-router';
import logoTextUrl from '@/assets/svg/logo-text.svg';

const appTitle = import.meta.env.VITE_APP_TITLE || 'Sanjuu Blog';

const props = withDefaults(
	defineProps<{
		size?: number | string;
		textColor?: string;
		showText?: boolean;
		alt?: string;
		to?: RouteLocationRaw;
	}>(),
	{
		size: '2rem',
		textColor: undefined,
		showText: true,
		alt: '网站 Logo',
		to: undefined,
	},
);

// 数字尺寸会转成 px；如果希望跟随根字体缩放，直接传 '2rem'、'2.5rem' 这类字符串。
const logoSize = computed(() => (typeof props.size === 'number' ? `${props.size}px` : props.size));

const logoStyle = computed(() => ({
	'--app-logo-size': logoSize.value,
	// 未传 textColor 时使用全局正文色；传入 textColor 时显式覆盖文字 Logo 颜色。
	'--app-logo-text-color': props.textColor || 'var(--app-text)',
	'--app-logo-text-aspect-ratio': '211 / 64',
	'--app-logo-text-url': `url("${logoTextUrl}")`,
}));

// 传入 to 时渲染成 RouterLink，可以点击跳转；不传 to 时渲染成普通 span。
// 静态字符串写法是 <AppLogo to="/" />；动态对象才写 :to="{ name: 'Home' }"。
const logoComponent = computed(() => (props.to ? RouterLink : 'span'));

// aria-label 是 HTML 的无障碍属性，视觉上不显示，但屏幕阅读器会读取。
const ariaLabel = computed(() => (props.showText ? appTitle : props.alt));
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

// BEM 命名：app-logo 是块，__mark / __text 是这个块里的元素。
.app-logo__mark {
	display: block;
	flex: none;
	object-fit: contain;
}

.app-logo__text {
	display: block;
	flex: none;
	height: var(--app-logo-size);
	aspect-ratio: var(--app-logo-text-aspect-ratio);
	background-color: var(--app-logo-text-color);
	// 用 SVG 做 mask 时，SVG 负责形状，background-color 负责颜色。
	mask: var(--app-logo-text-url) center / contain no-repeat;
	-webkit-mask: var(--app-logo-text-url) center / contain no-repeat;
}
</style>
