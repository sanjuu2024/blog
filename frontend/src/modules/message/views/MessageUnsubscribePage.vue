<template>
	<section class="message-unsubscribe">
		<div class="message-unsubscribe__panel">
			<i-material-symbols-unsubscribe-outline class="message-unsubscribe__icon" />
			<h1>关闭留言回复通知</h1>
			<p v-if="submitted">已关闭这条留言的后续通知。</p>
			<p v-else-if="!tokenValid">退订链接无效或已失效。</p>
			<p v-else>确认后，这条留言未来收到的管理员回复将不再发送邮件通知。</p>
			<el-button
				v-if="!submitted && tokenValid"
				type="primary"
				:loading="submitting"
				@click="handleUnsubscribe"
			>
				确认退订
			</el-button>
			<RouterLink
				v-if="submitted || !tokenValid"
				to="/messages"
				class="message-unsubscribe__link"
			>
				返回留言板
			</RouterLink>
		</div>
	</section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute } from 'vue-router';
import { unsubscribeMessage } from '../api/messageApi';

defineOptions({
	name: 'MessageUnsubscribePage',
});

const route = useRoute();
const token = computed(() =>
	typeof route.query.token === 'string' ? route.query.token.trim() : '',
);
const tokenValid = computed(() => token.value.length >= 20 && token.value.length <= 128);
const submitting = ref(false);
const submitted = ref(false);

async function handleUnsubscribe() {
	if (!tokenValid.value || submitting.value) return;
	submitting.value = true;
	try {
		await unsubscribeMessage({ token: token.value });
		submitted.value = true;
	} catch {
		// 请求错误由统一响应拦截器提示。
	} finally {
		submitting.value = false;
	}
}
</script>

<style scoped lang="scss">
.message-unsubscribe {
	display: grid;
	height: 100%;
	place-items: center;
}

.message-unsubscribe__panel {
	width: min(100%, 32rem);
	padding: 2rem;
	border: 1px solid var(--app-border);
	border-radius: 0.75rem;
	background: var(--app-surface);
	text-align: center;
}

.message-unsubscribe__icon {
	width: 2.5rem;
	height: 2.5rem;
	color: var(--app-main);
}

.message-unsubscribe__panel h1 {
	margin: 1rem 0 0.5rem;
	font-size: 1.35rem;
}

.message-unsubscribe__panel p {
	margin: 0 0 1.5rem;
	color: var(--app-text-muted);
}

.message-unsubscribe__link {
	color: var(--app-main);
}
</style>
