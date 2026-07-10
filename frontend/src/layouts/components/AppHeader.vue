<template>
	<header class="app-header shadow">
		<div class="app-header__inner">
			<div class="app-header__left">
				<AppLogo
					to="/"
					:show-text="false"
					class="app-header__logo"
				/>
				<nav
					aria-label="主导航"
					class="app-header__nav"
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
				<RouterLink
					v-for="r in rightNavItems"
					:key="r.path"
					:to="r.path"
					class="app-header__link"
				>
					{{ r.meta.title }}
				</RouterLink>
				<RouterLink
					to="/admin"
					class="app-header__link"
					v-if="userStore.userInfo?.role === 'ADMIN'"
				>
					后台
				</RouterLink>
				<el-dropdown
					placement="bottom-end"
					trigger="hover"
					popper-class="app-header-user-dropdown"
				>
					<button
						class="app-header__avatar"
						:class="{ 'app-header__avatar--pointer-hover': avatarPointerHover }"
						:aria-label="userStore.userInfo ? '打开用户菜单' : '打开登录菜单'"
						@pointerenter="handleAvatarPointerEnter"
						@pointerleave="handleAvatarPointerLeave"
						@focus="handleAvatarFocus"
						@keydown="handleAvatarKeyboard"
					>
						<AppUserAvatar
							v-if="userStore.userInfo"
							:avatar-url="userStore.userInfo?.avatarUrl"
							:name="userStore.userInfo?.nickname"
							:user-id="userStore.userInfo?.id"
							:size="40"
						/>
						<i-lucide-user-round
							v-else
							class="app-header__guest-icon"
						/>
					</button>
					<template #dropdown>
						<el-dropdown-menu>
							<template v-if="userStore.userInfo">
								<el-dropdown-item @click="router.push('/users/me')">
									<i-lucide-user-round />
									<span>个人中心</span>
								</el-dropdown-item>
								<el-dropdown-item @click="router.push('/users/me/settings')">
									<i-lucide-settings />
									<span>个人资料设置</span>
								</el-dropdown-item>
								<el-dropdown-item @click="openLogoutDialog">
									<i-lucide-log-out />
									<span>退出登录</span>
								</el-dropdown-item>
							</template>
							<template v-else>
								<el-dropdown-item @click="router.push('/auth/login')">
									<i-lucide-log-in />
									<span>注册/登录</span>
								</el-dropdown-item>
							</template>
							<li
								class="app-header__theme-menu-item"
								role="presentation"
								@click.stop
							>
								<AppThemeSwitcher />
							</li>
						</el-dropdown-menu>
					</template>
				</el-dropdown>
			</div>
		</div>
	</header>
	<el-dialog
		v-model="logoutDialogVisible"
		title="退出登录"
		width="450px"
		:close-on-click-modal="!logoutSubmitting"
		:close-on-press-escape="!logoutSubmitting"
		:show-close="!logoutSubmitting"
	>
		<p>确认退出登录吗？</p>
		<template #footer>
			<el-button
				:disabled="logoutSubmitting"
				@click="logoutDialogVisible = false"
			>
				取消
			</el-button>
			<el-button
				type="primary"
				:loading="logoutSubmitting"
				@click="handleLogout"
			>
				确认
			</el-button>
		</template>
	</el-dialog>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { publicRoutes } from '@/router/routes';
import { useAuthStore } from '@/stores/authStore';
import { useUserStore } from '@/stores/userStore';

const userStore = useUserStore();
const authStore = useAuthStore();
const router = useRouter();
const logoutDialogVisible = ref(false);
const logoutSubmitting = ref(false);
const avatarPointerHover = ref(false);

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

function openLogoutDialog() {
	logoutDialogVisible.value = true;
}

function handleAvatarPointerEnter(event: PointerEvent) {
	avatarPointerHover.value = true;
	const trigger = event.currentTarget;

	// Element Plus Dropdown 会在 hover 打开时主动 focus trigger。
	// 鼠标悬浮不应该留下键盘焦点，所以等框架完成 focus 后再清掉。
	requestAnimationFrame(() => {
		if (trigger instanceof HTMLElement && document.activeElement === trigger) {
			trigger.blur();
		}
	});
}

function handleAvatarPointerLeave() {
	avatarPointerHover.value = false;
}

function handleAvatarFocus(event: FocusEvent) {
	const trigger = event.currentTarget;

	requestAnimationFrame(() => {
		if (trigger instanceof HTMLElement && trigger.matches(':focus-visible')) {
			avatarPointerHover.value = false;
		}
	});
}

function handleAvatarKeyboard() {
	avatarPointerHover.value = false;
}

async function handleLogout() {
	if (logoutSubmitting.value) return;

	logoutSubmitting.value = true;
	try {
		await authStore.logout();
	} catch {
		// logout 接口异常时，authStore.logout() 仍会在 finally 中清理本地登录态
		// 这里继续完成前台跳转
	} finally {
		logoutSubmitting.value = false;
		logoutDialogVisible.value = false;
	}
	ElMessage.success('已退出登录');
	router.replace('/');
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
	color: var(--app-button-bg);
	box-shadow: 0 2px 0 var(--app-button-bg);
}

.app-header__logo,
.app-header__avatar {
	display: flex;
	align-items: center;
	height: var(--app-header-height);
	padding-inline: 1.25rem;
}

.app-header__avatar {
	border: 0;
	background: transparent;
	cursor: pointer;
}

// 但这样键盘交互性不好。最后使用 handleAvatarPointerEnter 等函数解决。
// .app-header :deep(.el-dropdown),
// .app-header :deep(.el-tooltip__trigger) {
// 	// 否则会有黑色矩形焦点框（el-dropdown 导致的）
// 	outline: none;
// }

.app-header__guest-icon {
	box-sizing: border-box;
	width: 2.5rem;
	height: 2.5rem;
	padding: 0.5rem;
	border: 2px solid var(--app-border);
	border-radius: 999px;
	background-color: var(--app-surface);
	color: var(--app-text-muted);
	transition:
		border-color 0.3s ease,
		color 0.3s ease;
}

.app-header__avatar:hover .app-header__guest-icon {
	border-color: var(--app-button-bg);
	color: var(--app-button-bg);
}

:global(.app-header-user-dropdown) {
	border-radius: 0.75rem;
	color: var(--app-text);
	font-weight: bold;
	font-family: inherit;
}

:global(.app-header-user-dropdown .el-dropdown-menu) {
	padding-top: 1rem;
	border-radius: 0.75rem;
	overflow: hidden;
}

:global(.app-header-user-dropdown .el-dropdown-menu__item) {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	color: var(--app-text-muted);
	font-family: inherit;
	font-weight: var(--app-button-font-weight);
	line-height: normal;
	padding-block: 0.5rem;
}

:global(.app-header-user-dropdown .el-dropdown-menu__item:not(.is-disabled):hover),
:global(.app-header-user-dropdown .el-dropdown-menu__item:not(.is-disabled):focus) {
	background-color: var(--el-dropdown-menuItem-hover-fill);
	color: var(--el-dropdown-menuItem-hover-color);
}

:global([data-theme='dark'] .app-header-user-dropdown .el-dropdown-menu__item) {
	color: var(--app-text-muted);
}

:global(
	[data-theme='dark'] .app-header-user-dropdown .el-dropdown-menu__item:not(.is-disabled):hover
),
:global(
	[data-theme='dark'] .app-header-user-dropdown .el-dropdown-menu__item:not(.is-disabled):focus
) {
	background-color: #3a3a3a;
	color: var(--app-text);
}

:global(.app-header-user-dropdown .app-header__theme-menu-item) {
	padding: 1.2rem 1rem;
	margin-block: 1rem;
	border-top: 1px solid var(--app-border);
	list-style: none;
}
</style>
