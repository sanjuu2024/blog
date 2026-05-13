<template>
	<div class="flex min-h-screen items-center justify-center p-4">
		<!-- 105 = 420px -->
		<div class="w-full max-w-105">
			<div class="mb-6 flex justify-center">
				<AppLogo :size="70" />
			</div>
			<router-view></router-view>
		</div>
	</div>
</template>

<script setup lang="ts">
import AppLogo from '@/components/AppLogo.vue';

defineOptions({
	name: 'AuthLayout',
});
</script>

<style></style>
