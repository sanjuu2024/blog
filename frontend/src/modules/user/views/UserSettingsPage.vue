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
					<h2 class="settings-section__title">公开资料</h2>
					<p class="settings-section__description">
						这些信息会展示在个人中心和文章作者资料中。
					</p>
				</div>
				<div class="avatar-setting">
					<AppUserAvatar
						:avatar-url="avatarDraftUrl || userProfile.avatarUrl"
						:name="userProfile.nickname || userProfile.username"
						:user-id="userProfile.id"
						:is-admin="userProfile.role === 'ADMIN'"
						:size="88"
						class="shrink-0"
					/>
					<div>
						<p class="avatar-setting__label">头像</p>
						<!-- 选择文件后先进入裁剪，确认结果仍只是未保存的资料草稿。 -->
						<el-upload
							:accept="IMAGE_ACCEPT"
							:auto-upload="false"
							:disabled="profileSubmitting"
							:show-file-list="false"
							:on-change="handleAvatarFileChange"
						>
							<el-button :disabled="profileSubmitting">
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
				<AvatarCropDialog
					v-model="avatarCropVisible"
					:source-file="avatarSourceFile"
					@confirm="setAvatarDraft"
					@error="ElMessage.error('图片读取失败，请重新选择')"
					@closed="avatarSourceFile = null"
				/>
			</section>

			<section class="settings-section">
				<div class="settings-section__header">
					<h2 class="settings-section__title">邮箱设置</h2>
					<p class="settings-section__description">修改邮箱后需要重新登录。</p>
				</div>

				<div class="security-item">
					<div>
						<p class="security-item__label">邮箱</p>
						<p class="security-item__value">{{ userProfile.email }}</p>
					</div>
				</div>

				<el-form
					:ref="setEmailFormRef"
					:model="emailForm"
					:rules="emailRules"
					label-position="top"
					class="settings-form email-form"
				>
					<el-form-item
						prop="newEmail"
						label="新邮箱"
					>
						<el-input
							v-model="emailForm.newEmail"
							maxlength="255"
							placeholder="请输入新邮箱"
						>
							<template #prefix>
								<i-lucide-mail />
							</template>
						</el-input>
					</el-form-item>
					<el-form-item
						prop="verificationCode"
						label="新邮箱验证码"
					>
						<div class="verification-code-field">
							<el-input
								v-model="emailForm.verificationCode"
								maxlength="6"
								placeholder="请输入 6 位验证码"
								inputmode="numeric"
								autocomplete="one-time-code"
							>
								<template #prefix>
									<i-lucide-key-round />
								</template>
							</el-input>
							<el-button
								class="verification-code-button"
								native-type="button"
								:type="emailTargetValidated ? 'success' : 'info'"
								:loading="emailCodeSending"
								:disabled="
									emailCodeSending ||
									emailCodeRemainingSeconds > 0 ||
									!emailTargetValidated
								"
								@click="sendUserEmailChangeCode"
							>
								<i-lucide-send
									v-if="!emailCodeSending && emailCodeRemainingSeconds === 0"
									class="mr-1"
								/>
								{{
									emailCodeRemainingSeconds > 0
										? `${emailCodeRemainingSeconds} 秒后重试`
										: '发送验证码'
								}}
							</el-button>
						</div>
					</el-form-item>
					<el-form-item
						prop="currentPassword"
						label="当前密码"
					>
						<el-input
							v-model="emailForm.currentPassword"
							type="password"
							maxlength="32"
							show-password
							placeholder="请输入当前密码"
						>
							<template #prefix>
								<i-lucide-lock-keyhole />
							</template>
						</el-input>
					</el-form-item>
					<div class="settings-form__actions">
						<el-button
							:disabled="emailSubmitting"
							@click="resetEmailForm"
						>
							重置
						</el-button>
						<el-button
							type="primary"
							:loading="emailSubmitting"
							:disabled="emailSubmitting || !emailValidated"
							@click="changeUserSettingsEmail"
						>
							修改邮箱
						</el-button>
					</div>
				</el-form>
			</section>

			<section class="settings-section">
				<div class="settings-section__header">
					<h2 class="settings-section__title">密码设置</h2>
					<p class="settings-section__description">
						修改密码后，其他设备上的登录状态也会失效。
					</p>
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
						>
							<template #prefix>
								<i-lucide-lock-keyhole-open />
							</template>
						</el-input>
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
						>
							<template #prefix>
								<i-lucide-lock-keyhole />
							</template>
						</el-input>
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
						>
							<template #prefix>
								<i-lucide-lock-keyhole />
							</template>
						</el-input>
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

			<section class="settings-section danger-zone">
				<div class="settings-section__header">
					<h2 class="settings-section__title">注销账号</h2>
					<p class="settings-section__description">
						注销后账号无法恢复，历史内容仍会保留并显示为“账号已注销”。
					</p>
				</div>
				<el-button
					type="danger"
					@click="handleDeleteAccount"
				>
					<i-lucide-power class="mr-1" />
					注销账号
				</el-button>
			</section>
		</template>
	</div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox, type UploadProps } from 'element-plus';
import 'element-plus/es/components/message-box/style/css';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppUserAvatar from '@/components/AppUserAvatar.vue';
import { AVATAR_MAX_SIZE, IMAGE_ACCEPT, validateImageFile } from '@/modules/file/utils/image';
import AvatarCropDialog from '../components/AvatarCropDialog.vue';
import { useUserSettings } from '../composables/useUserSettings';

defineOptions({
	name: 'UserSettingsPage',
});

const router = useRouter();

const {
	userProfile,
	profileForm,
	passwordForm,
	emailForm,
	profileRules,
	passwordRules,
	emailRules,
	profileValidated,
	profileChanged,
	passwordValidated,
	emailValidated,
	profileLoading,
	profileLoaded,
	profileLoadFailed,
	avatarDraftUrl,
	profileSubmitting,
	passwordSubmitting,
	emailCodeSending,
	emailSubmitting,
	emailCodeRemainingSeconds,
	emailTargetValidated,
	setProfileFormRef,
	setPasswordFormRef,
	setEmailFormRef,
	getUserSettingsProfile,
	setAvatarDraft,
	updateUserSettingsProfile,
	changeUserSettingsPassword,
	sendUserEmailChangeCode,
	changeUserSettingsEmail,
	deleteUserSettingsAccount,
	resetPasswordForm,
	resetEmailForm,
} = useUserSettings();

const avatarCropVisible = ref(false);
const avatarSourceFile = ref<File | null>(null);

// el-upload 只负责选择文件，原图通过校验后交给裁剪弹窗，不会立即上传。
const handleAvatarFileChange: UploadProps['onChange'] = (uploadFile) => {
	if (!uploadFile.raw) return;
	const message = validateImageFile(uploadFile.raw, AVATAR_MAX_SIZE);
	if (message) {
		ElMessage.warning(message);
		return;
	}
	avatarSourceFile.value = uploadFile.raw;
	avatarCropVisible.value = true;
};

async function handleDeleteAccount() {
	try {
		const { value } = await ElMessageBox.prompt(
			'注销后将立即释放用户名和邮箱，历史评论、留言和点赞会保留，账号无法恢复。',
			'注销账号',
			{
				confirmButtonText: '确认注销',
				cancelButtonText: '取消',
				confirmButtonClass: 'el-button--danger',
				inputType: 'password',
				inputPlaceholder: '请输入当前密码',
				inputValidator: (value) => (value ? true : '请输入当前密码'),
			},
		);
		await deleteUserSettingsAccount(value);
	} catch {
		// 用户取消确认或请求错误由请求拦截器统一处理
	}
}

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
		font: inherit;
		font-weight: 500;
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

.verification-code-field {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 0.5rem;
	width: 100%;
}

// “发送验证码”按钮需要自定义样式，否则深色模式下样式突兀
.verification-code-button.is-disabled {
	--el-button-disabled-bg-color: var(--app-surface-muted);
	--el-button-disabled-border-color: var(--app-border);
	--el-button-disabled-text-color: var(--app-text-disabled);

	opacity: 1;
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

		// 防止邮箱过长窄屏下溢出
		overflow-wrap: anywhere;
		white-space: pre-wrap;
	}
}

@media (width < 768px) {
	:deep(.el-form-item) {
		margin-bottom: 32px;
	}
}

@media (width < 376px) {
	.avatar-setting {
		display: flex;
		flex-direction: column;
		text-align: center;
	}

	.verification-code-field {
		flex-direction: column;
		align-items: stretch;
		gap: 0.5rem;
	}
}
</style>
