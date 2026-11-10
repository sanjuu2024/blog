<template>
	<main class="not-found-page">
		<img
			class="not-found-page__illustration"
			:src="notFoundImage"
			alt=""
		/>
		<div class="not-found-page__text">
			<p class="not-found-page__code">404</p>
			<h1 class="not-found-page__title">页面不存在</h1>
			<p class="not-found-page__description">你访问的页面可能已被移除，或地址有误。</p>
		</div>
		<div class="not-found-page__actions">
			<el-button
				class="not-found-page__secondary-button"
				:style="secondaryButtonStyle"
				@click="router.back()"
			>
				返回上一页
			</el-button>
			<el-button
				type="primary"
				@click="router.push('/')"
			>
				返回首页
			</el-button>
		</div>
	</main>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const notFoundImage = '/img/404.avif';
const router = useRouter();
const secondaryButtonStyle = {
	'--el-button-bg-color': '#fff',
	'--el-button-border-color': '#c5d1ce',
	'--el-button-text-color': '#263238',
	'--el-button-hover-bg-color': '#edf3f1',
	'--el-button-hover-border-color': '#c5d1ce',
	'--el-button-hover-text-color': '#263238',
	'--el-button-active-bg-color': '#edf3f1',
	'--el-button-active-border-color': '#c5d1ce',
	'--el-button-active-text-color': '#263238',
};

defineOptions({
	name: 'NotFound',
});
</script>

<style scoped lang="scss">
.not-found-page {
	display: grid;
	min-height: 100vh;
	place-content: center;
	justify-items: center;
	background-color: #f0ead9;
	color: #2d2d2d;
	text-align: center;
}

.not-found-page__illustration {
	display: block;
	width: min(100%, 20rem);
	height: 20rem;
	object-fit: contain;
}

.not-found-page__text {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 0.5rem;
	margin-block: 1.5rem;

	.not-found-page__code {
		color: #4f4f4f;
	}

	.not-found-page__title {
		font-size: 1.5rem;
		font-weight: bold;
	}

	.not-found-page__description {
		color: #4f4f4f;
	}
}

.not-found-page__actions {
	display: flex;
	flex-wrap: wrap;
	justify-content: center;
	gap: 0.75rem;

	:deep(.el-button) {
		height: auto;
		padding: 0.75rem 1rem;
		border-radius: 6px;
		font-family: inherit;
	}
}
</style>
