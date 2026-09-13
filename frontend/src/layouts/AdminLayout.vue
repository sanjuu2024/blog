<template>
	<div
		class="admin-layout"
		:style="adminLayoutStyle"
	>
		<AdminHeader />
		<AdminSidebar v-model:collapsed="isSidebarCollapsed" />
		<AdminMain />
	</div>
</template>

<script setup lang="ts">
import { computed, ref, type CSSProperties } from 'vue';
import AdminHeader from './components/AdminHeader.vue';
import AdminMain from './components/AdminMain.vue';
import AdminSidebar from './components/AdminSidebar.vue';

defineOptions({
	name: 'AdminLayout',
});

const isSidebarCollapsed = ref(false);

const adminLayoutStyle = computed<CSSProperties>(() => ({
	'--app-admin-sidebar-width': isSidebarCollapsed.value
		? 'var(--app-admin-min-sidebar-width)'
		: 'var(--app-admin-max-sidebar-width)',
}));
</script>

<style>
.admin-layout {
	position: relative;
	height: 100dvh;
	overflow: hidden;
}

@media (width < 768px) {
	:root {
		--app-admin-min-sidebar-width: 0px;
		--app-admin-max-sidebar-width: 0px;
	}
}
</style>
