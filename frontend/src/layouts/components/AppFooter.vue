<template>
	<footer class="app-footer">
		<div class="app-footer__inner">
			<span class="app-footer__copyright"
				>Copyright © 2026-{{ currentYear }} {{ siteName }}. All Rights Reserved.</span
			>
			<a
				class="app-footer__rss flex items-center"
				href="/rss.xml"
				type="application/rss+xml"
			>
				<i-streamline-ultimate-color-wifi-signal-2 class="mr-1" />
				RSS 订阅
			</a>
			<span
				v-if="uptime"
				class="app-footer__uptime"
			>
				🎉 本站已运行 {{ uptime.days }} 天 {{ uptime.hours }} 小时 {{ uptime.minutes }} 分
				{{ uptime.seconds }} 秒 ♪(´▽｀)
			</span>
		</div>
	</footer>
</template>

<script setup lang="ts">
import { useNow } from '@vueuse/core';
import dayjs from 'dayjs';
import { computed } from 'vue';

defineOptions({
	name: 'AppFooter',
});

const siteName = import.meta.env.VITE_APP_TITLE || 'Sanjuu Blog';
const currentYear = new Date().getFullYear();
const currentTime = useNow({ interval: 1000 });
const launchTime = computed(() => dayjs(import.meta.env.VITE_SITE_LAUNCHED_AT || ''));
const uptime = computed(() => {
	if (!launchTime.value.isValid()) return null;

	const elapsedSeconds = Math.max(
		0,
		Math.floor(currentTime.value.getTime() / 1000 - launchTime.value.unix()),
	);
	const days = Math.floor(elapsedSeconds / 86400);
	const hours = Math.floor((elapsedSeconds % 86400) / 3600);
	const minutes = Math.floor((elapsedSeconds % 3600) / 60);
	const seconds = elapsedSeconds % 60;
	const pad = (value: number) => String(value).padStart(2, '0');

	return {
		days,
		hours: pad(hours),
		minutes: pad(minutes),
		seconds: pad(seconds),
	};
});
</script>

<style scoped lang="scss">
.app-footer {
	height: var(--app-footer-height);
	border-top: 1px solid var(--app-border);
	display: flex;
	align-items: center;
	justify-content: center;
	background-color: var(--app-footer-bg);
	color: var(--app-footer-text);
}

.app-footer__inner {
	width: min(100% - var(--app-page-padding-x) * 2, var(--app-container-width));
	height: 100%;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	text-align: center;
	gap: 0.75rem;
}

.app-footer__rss {
	text-decoration: underline;

	&:hover,
	&:focus-visible {
		color: var(--app-main);
	}
}

@media (width < 400px) {
	.app-footer__uptime {
		font-size: 0.8rem;
	}
}
</style>
