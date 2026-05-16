<template>
	<header class="admin-header">
		<div class="admin-header__inner">
			<el-breadcrumb :separator-icon="ArrowRight">
				<template
					v-for="r in route.matched"
					:key="r.name"
				>
					<el-breadcrumb-item
						:to="r.path"
						class="align-middle"
					>
						<el-icon
							class="mx-2 align-[-2px]"
							size="1.1rem"
							v-if="getRouteIcon(r.meta?.icon)"
						>
							<component :is="getRouteIcon(r.meta?.icon)" />
						</el-icon>
						<span class="mx-1">{{ r.meta.title }}</span>
					</el-breadcrumb-item>
				</template>
			</el-breadcrumb>
		</div>
	</header>
</template>

<script setup lang="ts">
import ArrowRight from '~icons/ep/arrow-right-bold';
import { useRoute } from 'vue-router';
import getRouteIcon from '@/utils/getRouteIcon';

defineOptions({
	name: 'AdminHeader',
});

const route = useRoute();
</script>

<style scoped lang="scss">
.admin-header {
	position: sticky;
	top: 0;
	left: var(--app-admin-sidebar-width);
	width: calc(100% - var(--app-admin-sidebar-width));
	height: var(--app-admin-header-height);
	background-color: var(--app-surface);
	font-weight: 550;
	font-size: 1.1rem;
	transition:
		left 0.2s ease,
		width 0.2s ease;
}

.admin-header__inner {
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 100%;
	padding-inline: var(--app-main-padding-x);

	:deep(.el-breadcrumb) {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 100%;
		font-size: 1rem;
	}
}
</style>
