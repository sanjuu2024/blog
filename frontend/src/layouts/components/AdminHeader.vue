<template>
	<header class="admin-header shadow">
		<div class="admin-header__inner">
			<div class="admin-header__location">
				<!-- 桌面端面包屑 -->
				<el-breadcrumb
					:separator-icon="ArrowRight"
					class="admin-header__breadcrumb"
				>
					<template
						v-for="r in route.matched"
						:key="r.name"
					>
						<el-breadcrumb-item
							:to="r.path"
							class="align-middle"
						>
							<div class="flex items-center">
								<el-icon
									class="mx-2 align-[-2px]"
									size="1.1rem"
									v-if="getRouteIcon(r.meta?.icon)"
								>
									<component :is="getRouteIcon(r.meta?.icon)" />
								</el-icon>
								<span class="mx-1">{{ r.meta.title }}</span>
							</div>
						</el-breadcrumb-item>
					</template>
				</el-breadcrumb>

				<!-- 移动端标题 -->
				<div class="admin-header__mobile-title">
					<component
						:is="getRouteIcon(route.meta.icon)"
						v-if="getRouteIcon(route.meta.icon)"
					/>
					<span>{{ currentPageTitle }}</span>
				</div>
			</div>

			<div class="admin-header__right">
				<!-- 移动端导航 -->
				<div class="admin-header__mobile-nav">
					<el-dropdown
						trigger="click"
						placement="bottom-end"
						@command="handleNavigation"
					>
						<button
							type="button"
							class="admin-header__mobile-menu-button"
							aria-label="展开后台导航菜单"
						>
							<i-lucide-list />
						</button>

						<template #dropdown>
							<el-dropdown-menu>
								<el-dropdown-item command="/">
									<AppLogo
										class="mr-2"
										size="1.1rem"
										:show-text="false"
									/>
									前台
								</el-dropdown-item>
								<el-dropdown-item
									v-for="route in adminRoutes"
									:key="route.path"
									:command="route.path"
								>
									<component
										v-if="getRouteIcon(route.meta?.icon)"
										:is="getRouteIcon(route.meta?.icon)"
										class="mr-2"
										size="1.1rem"
									/>
									{{ route.meta.title }}
								</el-dropdown-item>
							</el-dropdown-menu>
						</template>
					</el-dropdown>
				</div>

				<!-- 头像下拉菜单 -->
				<AppUserMenu />
			</div>
		</div>
	</header>
</template>

<script setup lang="ts">
import ArrowRight from '~icons/ep/arrow-right-bold';
import { useRoute } from 'vue-router';
import getRouteIcon from '@/utils/getRouteIcon';
import { adminRoutes } from '@/router/routes';
import { useRouter } from 'vue-router';
import { computed } from 'vue';

defineOptions({
	name: 'AdminHeader',
});

const route = useRoute();
const router = useRouter();

const currentPageTitle = computed(() => {
	const matchedRoute = [...route.matched]
		.reverse()
		.find((item) => typeof item.meta.title === 'string');

	return matchedRoute?.meta.title ?? '后台管理';
});

function handleNavigation(path: string) {
	router.push(path);
}
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
	z-index: 9;
}

.admin-header__inner {
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 100%;
	padding-inline: 1rem;

	:deep(.el-breadcrumb) {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 100%;
		font-size: 1rem;
	}

	.admin-header__location {
		min-width: 0;
		flex: 1;
		overflow: hidden;
		display: flex;
		align-items: center;
	}
}

.admin-header__breadcrumb {
	white-space: nowrap;
}

.admin-header__mobile-title {
	display: none;
	gap: 0.5rem;

	span {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
}

.admin-header__right {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 1.25rem;
}

.admin-header__mobile-nav {
	display: none;
}

.admin-header__mobile-menu-button {
	font-size: 1.5rem;
	cursor: pointer;

	:hover,
	:focus-visible {
		color: var(--app-main);
	}
}

@media (width < 768px) {
	.admin-header__mobile-nav {
		display: flex;
	}

	.admin-header__inner .admin-header__breadcrumb {
		display: none;
	}

	.admin-header__mobile-title {
		display: flex;
		align-items: center;
		justify-content: center;
	}
}
</style>
