<template>
	<RouterView #default="{ Component }">
		<KeepAlive include="AdminArticleListPage">
			<component :is="Component" />
		</KeepAlive>
	</RouterView>
</template>

<script setup lang="ts">
defineOptions({
	name: 'AdminArticleLayout',
});
</script>
