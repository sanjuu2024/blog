<template>
	<div class="app-layout">
		<AppHeader />
		<AppMain />
	</div>
</template>

<script setup lang="ts">
import AppHeader from './components/AppHeader.vue';
import AppMain from './components/AppMain.vue';

defineOptions({
	name: 'MainLayout',
});
</script>

<style>
.app-layout {
	height: 100dvh;
	overflow-y: scroll;
}
</style>
