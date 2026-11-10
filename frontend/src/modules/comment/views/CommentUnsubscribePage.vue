<template>
	<section class="comment-unsubscribe">
		<div class="comment-unsubscribe__panel">
			<i-material-symbols-unsubscribe-outline class="comment-unsubscribe__icon" />
			<h1>关闭评论回复通知</h1>
			<p v-if="submitted">已关闭这条评论的后续通知。</p>
			<p v-else-if="!tokenValid">退订链接无效或已失效。</p>
			<p v-else>确认后，这条评论未来收到的直接回复将不再发送邮件通知。</p>
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
				to="/articles"
				class="comment-unsubscribe__link"
			>
				返回文章列表
			</RouterLink>
		</div>
	</section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute } from 'vue-router';
import { unsubscribeComment } from '../api/commentApi';

defineOptions({
	name: 'CommentUnsubscribePage',
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
		await unsubscribeComment({ token: token.value });
		submitted.value = true;
	} catch {
		// 请求错误由统一响应拦截器提示。
	} finally {
		submitting.value = false;
	}
}
</script>

<style scoped lang="scss">
.comment-unsubscribe {
	display: grid;
	height: 100%;
	place-items: center;
}

.comment-unsubscribe__panel {
	width: min(100%, 32rem);
	padding: 2rem;
	border: 1px solid var(--app-border);
	border-radius: 0.75rem;
	background: var(--app-surface);
	text-align: center;
}

.comment-unsubscribe__icon {
	width: 2.5rem;
	height: 2.5rem;
	color: var(--app-main);
}

.comment-unsubscribe__panel h1 {
	margin: 1rem 0 0.5rem;
	font-size: 1.35rem;
}

.comment-unsubscribe__panel p {
	margin: 0 0 1.5rem;
	color: var(--app-text-muted);
}

.comment-unsubscribe__link {
	color: var(--app-main);
}
</style>
