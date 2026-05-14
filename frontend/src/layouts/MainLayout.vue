<template>
	<AppHeader />
	<AppMain />
</template>

<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue';
import AppMain from './components/AppMain.vue';

defineOptions({
	name: 'MainLayout',
});
</script>

<style></style>
