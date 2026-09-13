<template>
	<header class="app-header shadow">
		<div class="app-header__inner">
			<div class="app-header__left">
				<AppLogo
					to="/"
					:show-text="false"
					class="app-header__logo"
				/>
				<!-- 桌面端导航 -->
				<nav
					aria-label="主导航"
					class="app-header__nav app-header__desktop-nav"
				>
					<RouterLink
						v-for="r in leftNavItems"
						:key="r.path"
						:to="r.path"
						class="app-header__link"
					>
						{{ r.meta.title }}
					</RouterLink>
				</nav>
			</div>

			<div class="app-header__right">
				<!-- 桌面端导航 -->
				<RouterLink
					v-for="r in rightNavItems"
					:key="r.path"
					:to="r.path"
					class="app-header__link app-header__desktop-nav"
				>
					{{ r.meta.title }}
				</RouterLink>
				<RouterLink
					to="/admin"
					class="app-header__link app-header__desktop-nav"
					v-if="userStore.userInfo?.role === 'ADMIN'"
				>
					后台
				</RouterLink>

				<!-- 移动端导航 -->
				<div class="app-header__mobile-nav">
					<el-dropdown
						trigger="click"
						placement="bottom-end"
						@command="handleNavigation"
					>
						<button
							type="button"
							class="app-header__mobile-menu-button"
							aria-label="打开主导航"
						>
							<i-lucide-list class="mx-3" />
						</button>

						<template #dropdown>
							<el-dropdown-menu>
								<el-dropdown-item
									v-for="route in mobileNavItems"
									:key="route.path"
									:command="route.path"
								>
									{{ route.meta.title }}
								</el-dropdown-item>
								<el-dropdown-item
									v-if="userStore.userInfo?.role === 'ADMIN'"
									command="/admin"
								>
									后台
								</el-dropdown-item>
							</el-dropdown-menu>
						</template>
					</el-dropdown>
				</div>

				<!-- 头像下拉菜单 -->
				<AppUserMenu :px-rem="isMobile ? 0.75 : 1.25" />
			</div>
		</div>
	</header>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { publicRoutes } from '@/router/routes';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/userStore';
import { useMediaQuery } from '@vueuse/core';

const isMobile = useMediaQuery('(width < 768px)');

const router = useRouter();
const userStore = useUserStore();

defineOptions({
	name: 'AppHeader',
});

// 左侧路由
const leftNavItems = computed(() =>
	publicRoutes.filter((route) => {
		if (route.meta?.nav?.pos !== 'left') return false;
		return true;
	}),
);

// 右侧路由
const rightNavItems = computed(() =>
	publicRoutes.filter((route) => {
		if (route.meta?.nav?.pos !== 'right') return false;
		return true;
	}),
);

// 移动端导航相关
const mobileNavItems = computed(() => [...leftNavItems.value, ...rightNavItems.value]);

function handleNavigation(path: string) {
	router.push(path);
}
</script>

<style lang="scss" scoped>
.app-header {
	position: sticky;
	top: 0;
	z-index: var(--app-header-z-index);
	width: 100%;
	height: var(--app-header-height);
	background-color: var(--app-surface);
	font-weight: 550;
	font-size: 1.1rem;
}

.app-header__inner {
	display: flex;
	align-items: center;
	justify-content: space-between;
	// 当容器达到最大宽度后，让 app-header 在大屏居中
	width: min(100% - var(--app-page-padding-x) * 2, var(--app-container-width));
	height: 100%;
	margin-inline: auto;
}

.app-header__left,
.app-header__right,
.app-header__nav {
	display: flex;
	align-items: center;
}

.app-header__link {
	display: flex;
	align-items: center;
	height: var(--app-header-height);
	padding-inline: 1.25rem;
	transition:
		color 0.3s ease,
		box-shadow 0.3s ease;
}

.app-header__link:hover,
.app-header__link.router-link-active {
	color: var(--app-main);
	box-shadow: 0 2px 0 var(--app-main);
}

.app-header__logo {
	display: flex;
	align-items: center;
	height: var(--app-header-height);
	padding-inline: 1.25rem;
}

// 但这样键盘交互性不好。最后使用 handleAvatarPointerEnter 等函数解决。
// .app-header :deep(.el-dropdown),
// .app-header :deep(.el-tooltip__trigger) {
// 	// 否则会有黑色矩形焦点框（el-dropdown 导致的）
// 	outline: none;
// }

.app-header__mobile-nav {
	display: none;
}

.app-header__mobile-menu-button {
	font-size: 1.5rem;
	cursor: pointer;

	:hover,
	:focus-visible {
		color: var(--app-main);
	}
}

@media (width < 768px) {
	.app-header__desktop-nav {
		display: none;
	}

	.app-header__mobile-nav {
		display: flex;
	}
}
</style>
