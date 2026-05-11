<template>
	<el-config-provider :locale="zhCn">
		<h1>这里是 {{ appTitle }} ，你好。</h1>
		<i-ep-edit />
		<i-ep-user />
	</el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn';

defineOptions({
	name: 'App',
});

const appTitle = import.meta.env.VITE_APP_TITLE;
</script>

<style></style>
