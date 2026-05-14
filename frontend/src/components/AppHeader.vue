<template>
	<header class="app-header">
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
						to="/home"
						class="app-header__link"
						>首页</RouterLink
					>
					<RouterLink
						to="/articles"
						class="app-header__link"
						>文章</RouterLink
					>
					<RouterLink
						to="/categories"
						class="app-header__link"
						>分类</RouterLink
					>
					<RouterLink
						to="/links"
						class="app-header__link"
						>友链</RouterLink
					>
				</nav>
			</div>
			<div class="app-header__right">
				<RouterLink
					to="/about"
					class="app-header__link"
					>关于</RouterLink
				>
				<RouterLink
					to="/users/me"
					class="app-header__avatar"
				>
					<el-avatar :size="40">
						<i-ep-user-filled class="text-xl text-white" />
					</el-avatar>
				</RouterLink>
			</div>
		</div>
	</header>
</template>

<script setup lang="ts">
defineOptions({
	name: 'AppHeader',
});
</script>

<style lang="scss" scoped>
.app-header {
	position: sticky;
	top: 0;
	z-index: var(--app-header-z-index);
	width: 100%;
	height: var(--app-header-height);
	background-color: var(--app-surface);
	box-shadow: var(--app-header-shadow);
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
	padding-inline: 1.25rem;
}
</style>
