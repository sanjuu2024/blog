<template>
	<div class="message-turnstile">
		<div
			v-if="status === 'loading'"
			class="message-turnstile__state"
			data-test="turnstile-loading"
		>
			<i-lucide-loader class="animate-spin text-2xl text-(--app-text-muted)" />
		</div>
		<div
			v-show="status === 'ready'"
			ref="containerRef"
			class="message-turnstile__widget"
			data-test="turnstile-widget"
		></div>
		<div
			v-if="status === 'error'"
			class="message-turnstile__state message-turnstile__error"
			data-test="turnstile-error"
		>
			<span>{{ errorMessage }}</span>
			<el-button
				type="primary"
				size="small"
				data-test="turnstile-retry"
				@click="loadWidget"
			>
				<i-lucide-refresh-cw class="mr-1" />
				重试
			</el-button>
		</div>
	</div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useScriptTag } from '@vueuse/core';
import { useTheme } from '@/composables/useTheme';
import type { TurnstileRenderOptions } from '../types/turnstile';

const SCRIPT_URL = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
type TurnstileStatus = 'loading' | 'ready' | 'error';

defineOptions({
	name: 'MessageTurnstile',
});

const props = withDefaults(
	defineProps<{
		siteKey: string;
		action?: string;
	}>(),
	{
		action: 'guest_message',
	},
);

const emit = defineEmits<{
	verified: [token: string];
}>();

const { resolvedTheme } = useTheme();
const containerRef = ref<HTMLElement>();
const status = ref<TurnstileStatus>('loading');
const errorMessage = ref('');
let widgetId: string | number | null = null;
const { load: loadScript } = useScriptTag(SCRIPT_URL, undefined, { manual: true });

function removeWidget() {
	if (widgetId !== null && window.turnstile) {
		window.turnstile.remove(widgetId);
	}
	widgetId = null;
}

function showError(message: string) {
	status.value = 'error';
	errorMessage.value = message;
	// 错误状态继续保留外层占位，只清理已经失效的 Cloudflare iframe 实例。
	void nextTick().then(removeWidget);
}

async function loadWidget() {
	removeWidget();
	status.value = 'loading';
	errorMessage.value = '';
	if (!props.siteKey) {
		showError('人机验证暂不可用，请稍后重试');
		return;
	}

	try {
		await loadScript();
		status.value = 'ready';
		await nextTick();
		if (!containerRef.value || !window.turnstile) throw new Error('Turnstile unavailable');
		const options: TurnstileRenderOptions = {
			sitekey: props.siteKey,
			action: props.action,
			theme: resolvedTheme.value,
			size: 'flexible',
			callback: (token) => emit('verified', token),
			'error-callback': () => showError('人机验证失败'),
		};
		widgetId = window.turnstile.render(containerRef.value, options);
	} catch {
		showError('人机验证加载失败');
	}
}

watch(resolvedTheme, () => {
	if (widgetId !== null) {
		// Turnstile 不支持渲染后切换主题，需要销毁当前实例后重新渲染。
		void loadWidget();
	}
});

onMounted(() => void loadWidget());
onBeforeUnmount(removeWidget);
</script>

<style scoped lang="scss">
.message-turnstile {
	display: grid;
	width: min(300px, 100%);
	min-height: 65px;
	place-items: center;
}

.message-turnstile__state,
.message-turnstile__widget {
	grid-area: 1 / 1;
	width: 100%;
}

.message-turnstile__state {
	display: flex;
	min-height: 65px;
	align-items: center;
	justify-content: center;
}

.message-turnstile__error {
	gap: 0.5rem;
	color: var(--app-text-muted);
	font-size: 0.85rem;
}
</style>
