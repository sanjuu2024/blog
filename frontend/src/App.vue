<template>
	<el-config-provider :locale="zhCn">
		<router-view></router-view>
	</el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import { usePageSeo } from '@/modules/seo/composables/usePageSeo';

usePageSeo();

defineOptions({
	name: 'App',
});
</script>
