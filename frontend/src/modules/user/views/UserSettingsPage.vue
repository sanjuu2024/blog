<template>
	<div class="user-settings-page">
		<header class="user-settings-header">
			<div>
				<p class="user-settings-header__eyebrow">Settings</p>
				<h1 class="user-settings-header__title">设置</h1>
			</div>
			<el-button
				plain
				@click="router.push('/users/me')"
			>
				返回个人中心
				<template #icon>
					<i-lucide-arrow-left />
				</template>
			</el-button>
		</header>

		<div
			v-if="profileLoading || (!profileLoaded && !profileLoadFailed)"
			class="settings-state"
		>
			<i-lucide-loader class="animate-spin text-2xl text-(--app-text-muted)" />
			<span>正在加载设置...</span>
		</div>

		<div
			v-else-if="profileLoadFailed"
			class="settings-state"
		>
			<p>设置加载失败</p>
			<el-button
				type="primary"
				:loading="profileLoading"
				@click="getUserSettingsProfile"
			>
				重新加载
			</el-button>
		</div>

		<template v-else>
			<section class="settings-section">
				<div class="settings-section__header">
					<div>
						<h2 class="settings-section__title">公开资料</h2>
						<p class="settings-section__description">
							这些信息会展示在个人中心和文章作者资料中。
						</p>
					</div>
				</div>
				<div class="avatar-setting">
					<AppUserAvatar
						:avatar-url="userProfile.avatarUrl"
						:name="userProfile.nickname || userProfile.username"
						:user-id="userProfile.id"
						:size="88"
						class="shrink-0"
					/>
					<div>
						<p class="avatar-setting__label">头像</p>
						<!-- 点击内部按钮时由 el-upload 唤起文件选择器；选中文件后交给 on-change 自行上传 -->
						<el-upload
							:accept="IMAGE_ACCEPT"
							:auto-upload="false"
							:disabled="avatarUploading"
							:show-file-list="false"
							:on-change="handleAvatarFileChange"
						>
							<el-button
								:loading="avatarUploading"
								:disabled="avatarUploading"
							>
								更换头像
								<template #icon>
									<i-lucide-upload />
								</template>
							</el-button>
						</el-upload>
					</div>
				</div>
				<el-form
					:ref="setProfileFormRef"
					:model="profileForm"
					:rules="profileRules"
					label-position="top"
					class="settings-form"
				>
					<el-form-item
						prop="nickname"
						label="昵称"
					>
						<el-input
							v-model="profileForm.nickname"
							placeholder="请输入昵称"
						/>
					</el-form-item>
					<el-form-item
						prop="bio"
						label="简介"
					>
						<el-input
							v-model="profileForm.bio"
							type="textarea"
							:rows="4"
							maxlength="100"
							show-word-limit
							placeholder="请输入个人简介"
						/>
					</el-form-item>
					<el-button
						type="primary"
						:loading="profileSubmitting"
						:disabled="profileSubmitting || !profileValidated || !profileChanged"
						@click="updateUserSettingsProfile"
					>
						保存公开资料
					</el-button>
				</el-form>
			</section>

			<section class="settings-section">
				<div class="settings-section__header">
					<div>
						<h2 class="settings-section__title">账号安全</h2>
						<p class="settings-section__description">
							邮箱和密码用于登录与账号安全校验。
						</p>
					</div>
				</div>

				<div class="security-item">
					<div>
						<p class="security-item__label">邮箱</p>
						<p class="security-item__value">{{ userProfile.email }}</p>
					</div>
					<el-tag type="info">暂不支持修改</el-tag>
				</div>

				<el-form
					:ref="setPasswordFormRef"
					:model="passwordForm"
					:rules="passwordRules"
					label-position="top"
					class="settings-form"
				>
					<el-form-item
						prop="oldPassword"
						label="当前密码"
					>
						<el-input
							v-model="passwordForm.oldPassword"
							type="password"
							maxlength="32"
							show-password
							placeholder="请输入当前密码"
						/>
					</el-form-item>
					<el-form-item
						prop="newPassword"
						label="新密码"
					>
						<el-input
							v-model="passwordForm.newPassword"
							type="password"
							maxlength="32"
							show-password
							placeholder="请输入新密码"
						/>
					</el-form-item>
					<el-form-item
						prop="confirmPassword"
						label="确认新密码"
					>
						<el-input
							v-model="passwordForm.confirmPassword"
							type="password"
							maxlength="32"
							show-password
							placeholder="请再次输入新密码"
						/>
					</el-form-item>
					<div class="settings-form__actions">
						<el-button
							:disabled="passwordSubmitting"
							@click="resetPasswordForm"
						>
							重置
						</el-button>
						<el-button
							type="primary"
							:loading="passwordSubmitting"
							:disabled="passwordSubmitting || !passwordValidated"
							@click="changeUserSettingsPassword"
						>
							修改密码
						</el-button>
					</div>
				</el-form>
			</section>
		</template>
	</div>
</template>

<script setup lang="ts">
import type { UploadProps } from 'element-plus';
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import AppUserAvatar from '@/components/AppUserAvatar.vue';
import { IMAGE_ACCEPT } from '@/modules/file/utils/image';
import { useUserSettings } from '../composables/useUserSettings';

defineOptions({
	name: 'UserSettingsPage',
});

const router = useRouter();

const {
	userProfile,
	profileForm,
	passwordForm,
	profileRules,
	passwordRules,
	profileValidated,
	profileChanged,
	passwordValidated,
	profileLoading,
	profileLoaded,
	profileLoadFailed,
	avatarUploading,
	profileSubmitting,
	passwordSubmitting,
	setProfileFormRef,
	setPasswordFormRef,
	getUserSettingsProfile,
	updateUserSettingsAvatar,
	updateUserSettingsProfile,
	changeUserSettingsPassword,
	resetPasswordForm,
} = useUserSettings();

// el-upload 唤起文件选择器后，会把用户选中的文件包装成 UploadFile 并传入该函数
// 头像上传接口需要接收浏览器原始的 File，因此从 uploadFile.raw 中取出后再交给 composable
const handleAvatarFileChange: UploadProps['onChange'] = (uploadFile) => {
	if (uploadFile.raw) {
		void updateUserSettingsAvatar(uploadFile.raw);
	}
};

onMounted(() => {
	getUserSettingsProfile();
});
</script>

<style lang="scss" scoped>
.user-settings-page {
	width: min(960px, 100%);
	margin-inline: auto;
	padding: 1.5rem;
}

.user-settings-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 1rem;

	.user-settings-header__eyebrow {
		color: var(--app-text-muted);
		font-size: 0.8rem;
	}

	.user-settings-header__title {
		font-size: 1.6rem;
		font-weight: 700;
	}
}

.settings-section {
	margin-bottom: 1rem;
	padding: 1.25rem;
	border: 1px solid var(--app-border);
	border-radius: 0.7rem;
}

.settings-state {
	display: flex;
	min-height: 12rem;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 1rem;
	color: var(--app-text-muted);
}

.settings-section__header {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	margin-bottom: 1rem;

	.settings-section__title {
		font-size: 1.1rem;
		font-weight: 700;
	}

	.settings-section__description {
		margin-top: 0.25rem;
		color: var(--app-text-muted);
		font-size: 0.9rem;
	}
}

.avatar-setting {
	display: flex;
	align-items: center;
	gap: 1rem;
	margin-bottom: 1.25rem;

	.avatar-setting__label {
		margin-bottom: 0.5rem;
		font-weight: 600;
	}
}

.settings-form {
	:deep(.el-form-item__label) {
		color: inherit;
		font: inherit;
		line-height: normal;
	}

	:deep(.el-form-item__error) {
		margin-top: 4px;
		margin-left: 2px;
		line-height: 0.8rem;
	}

	.settings-form__actions {
		display: flex;
		justify-content: flex-end;
		gap: 0.75rem;
	}
}

.security-item {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 1rem;
	padding: 0.875rem 1rem;
	border: 1px solid var(--app-border);
	border-radius: 0.7rem;

	.security-item__label {
		color: var(--app-text-muted);
		font-size: 0.9rem;
	}

	.security-item__value {
		margin-top: 0.25rem;
		font-weight: 600;
	}
}

@media (width < 376px) {
	.avatar-setting {
		display: flex;
		flex-direction: column;
		text-align: center;
	}
}
</style>
