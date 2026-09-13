<template>
	<!-- 头像下拉菜单 -->
	<el-dropdown
		placement="bottom-end"
		trigger="hover"
		popper-class="app-header-user-dropdown"
	>
		<button
			class="app-header__avatar"
			:style="{
				'padding-inline': pxRem != null ? pxRem + 'rem' : undefined,
			}"
			:aria-label="userStore.userInfo ? '打开用户菜单' : '打开登录菜单'"
			@pointerenter="handleAvatarPointerEnter"
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

	<el-dialog
		v-model="logoutDialogVisible"
		title="退出登录"
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
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/authStore';
import { useUserStore } from '@/stores/userStore';

const authStore = useAuthStore();
const userStore = useUserStore();
const router = useRouter();

const logoutDialogVisible = ref(false);
const logoutSubmitting = ref(false);
function openLogoutDialog() {
	logoutDialogVisible.value = true;
}

withDefaults(
	defineProps<{
		pxRem?: number;
	}>(),
	{
		pxRem: 0,
	},
);

function handleAvatarPointerEnter(event: PointerEvent) {
	const trigger = event.currentTarget;

	// Element Plus Dropdown 会在 hover 打开时主动 focus trigger。
	// 鼠标悬浮不应该留下键盘焦点，所以等框架完成 focus 后再清掉。
	requestAnimationFrame(() => {
		if (trigger instanceof HTMLElement && document.activeElement === trigger) {
			trigger.blur();
		}
	});
}

async function handleLogout() {
	if (logoutSubmitting.value) return;

	logoutSubmitting.value = true;

	try {
		await authStore.logout();
		ElMessage.success('已退出登录');
		router.replace('/');
	} catch {
		ElMessage.warning('退出请求失败，请检查网络后重试');
		// authStore.logout() 的 finally 已经清除了本地登录态，所以还是跳转首页。
		router.replace('/');
	} finally {
		logoutSubmitting.value = false;
		logoutDialogVisible.value = false;
	}
}
</script>

<style scoped lang="scss">
.app-header__avatar {
	display: flex;
	align-items: center;
	height: var(--app-header-height);
}

.app-header__avatar {
	border: 0;
	background: transparent;
	cursor: pointer;
}

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
	border-color: var(--app-main);
	color: var(--app-main);
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
