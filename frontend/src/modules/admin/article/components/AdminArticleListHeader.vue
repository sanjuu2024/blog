<template>
	<div class="header flex items-center justify-between">
		<span class="text-xl font-bold">文章列表</span>
		<el-button
			type="primary"
			class="m-4"
			@click="router.push('/admin/articles/create')"
		>
			编写笔记
		</el-button>
	</div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

defineOptions({
	name: 'AdminArticleHeader',
});
</script>

<style></style>
