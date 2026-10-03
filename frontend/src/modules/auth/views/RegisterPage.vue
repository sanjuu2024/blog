<template>
	<el-card class="auth-card">
		<h1 class="mt-2 mb-4 text-center text-2xl">注册</h1>
		<hr class="mb-4 text-gray-300" />
		<el-form
			ref="registerFormRef"
			class="register-form"
			label-width="auto"
			label-position="top"
			:model="registerForm"
			:rules="rules"
			@submit.prevent="handlerRegister"
		>
			<el-form-item
				prop="username"
				label="用户名"
			>
				<el-input
					v-model="registerForm.username"
					maxlength="20"
					placeholder="请输入用户名"
				>
					<template #prefix>
						<i-lucide-user />
					</template>
				</el-input>
			</el-form-item>
			<el-form-item
				prop="email"
				label="邮箱"
			>
				<el-input
					v-model="registerForm.email"
					maxlength="255"
					placeholder="请输入邮箱"
				>
					<template #prefix>
						<i-lucide-mail />
					</template>
				</el-input>
			</el-form-item>
			<el-form-item
				prop="verificationCode"
				label="邮箱验证码"
			>
				<div class="verification-code-field">
					<el-input
						v-model="registerForm.verificationCode"
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
						:type="emailAvailable && remainingSeconds === 0 ? 'success' : 'info'"
						:loading="sendingCode"
						:disabled="!emailAvailable || sendingCode || remainingSeconds > 0"
						@click="sendCode"
					>
						<i-lucide-send
							v-if="!sendingCode && remainingSeconds === 0"
							class="mr-1"
						/>
						{{ remainingSeconds > 0 ? `${remainingSeconds} 秒后重试` : '发送验证码' }}
					</el-button>
				</div>
			</el-form-item>
			<el-form-item
				prop="password"
				label="密码"
			>
				<el-input
					v-model="registerForm.password"
					maxlength="32"
					placeholder="请输入密码"
					type="password"
					show-password
				>
					<template #prefix>
						<i-lucide-lock-keyhole />
					</template>
				</el-input>
			</el-form-item>
			<el-form-item
				prop="confirmPassword"
				label="确认密码"
			>
				<el-input
					v-model="registerForm.confirmPassword"
					maxlength="32"
					placeholder="请再次输入密码"
					type="password"
					show-password
				>
					<template #prefix>
						<i-lucide-lock-keyhole />
					</template>
				</el-input>
			</el-form-item>
			<el-button
				type="primary"
				native-type="submit"
				class="mb-4 w-full"
				:disabled="!validated"
			>
				创建账号
			</el-button>
		</el-form>

		<div class="mb-2 flex items-center gap-1 text-sm">
			<el-checkbox
				v-model="checked"
				class="auth-footer-checkbox"
			>
				我已知晓并同意
			</el-checkbox>
			<el-link
				type="primary"
				class="auth-footer-link"
				@click="privacyPolicyVisible = true"
				underline="always"
			>
				隐私政策
			</el-link>
		</div>
		<PrivacyPolicyDialog
			v-model="privacyPolicyVisible"
			@loaded="handlePrivacyPolicyLoaded"
		/>

		<div class="footer">
			<div class="links flex justify-between">
				<el-link
					type="primary"
					class="auth-footer-link"
					@click="router.replace('/auth/login')"
				>
					已有帐号？去登录
				</el-link>
				<el-link
					type="primary"
					class="auth-footer-link"
					@click="router.replace('/')"
				>
					返回首页
				</el-link>
			</div>
		</div>
	</el-card>
</template>

<script setup lang="ts">
import { nextTick, ref, reactive, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useIntervalFn } from '@vueuse/core';
import { isAxiosError } from 'axios';
import type { ApiResult } from '@/types/api';
import type { RegisterRequest } from '../types/auth';
import { register, sendEmailVerificationCode } from '../api/authApi';
import type { FormInstance, FormItemRule } from 'element-plus';
import { ElMessage } from 'element-plus';
import PrivacyPolicyDialog from '@/modules/privacy/components/PrivacyPolicyDialog.vue';
import { getPrivacyPolicy } from '@/modules/privacy/api/privacyApi';
import { ApiCode } from '@/constants/apiCode';
import {
	EMAIL_FORMAT_MESSAGE,
	EMAIL_FORMAT_PATTERN,
	EMAIL_VERIFICATION_CODE_MESSAGE,
	EMAIL_VERIFICATION_CODE_PATTERN,
	PASSWORD_FORMAT_MESSAGE,
	PASSWORD_FORMAT_PATTERN,
	USERNAME_FORMAT_MESSAGE,
	USERNAME_FORMAT_PATTERN,
} from '@/constants/validation';

defineOptions({
	name: 'RegisterPage',
});

const router = useRouter();
const registerFormRef = ref<FormInstance>();
const privacyPolicyVisible = ref(false);
const privacyPolicyVersion = ref('');

interface RegisterForm extends RegisterRequest {
	confirmPassword: string;
}

// 注册表单数据
let registerForm = reactive<RegisterForm>({
	username: '',
	email: '',
	password: '',
	confirmPassword: '',
	verificationCode: '',
	privacyPolicyVersion: '',
});

let usernameAvailable = ref<boolean>(false);
let emailAvailable = ref<boolean>(false);
let passwordValid = ref<boolean>(false);
let confirmPasswordValid = ref<boolean>(false);
let verificationCodeValid = ref<boolean>(false);
const checked = ref<boolean>(false);
const sendingCode = ref(false);
const remainingSeconds = ref(0);
const { pause: pauseCountdown, resume: resumeCountdown } = useIntervalFn(
	() => {
		if (remainingSeconds.value <= 1) {
			remainingSeconds.value = 0;
			pauseCountdown();
			return;
		}
		remainingSeconds.value -= 1;
	},
	1000,
	{ immediate: false },
);

// 表单校验规则
const rules = {
	username: [
		{
			required: true,
			trigger: 'change',
			validator: (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
				usernameAvailable.value = false;
				if (!USERNAME_FORMAT_PATTERN.test(value)) {
					callback(new Error(USERNAME_FORMAT_MESSAGE));
				} else {
					usernameAvailable.value = true;
					callback();
				}
			},
		},
	],
	email: [
		{
			required: true,
			trigger: 'change',
			validator: (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
				emailAvailable.value = false;
				if (!EMAIL_FORMAT_PATTERN.test(value)) {
					callback(new Error(EMAIL_FORMAT_MESSAGE));
				} else {
					emailAvailable.value = true;
					callback();
				}
			},
		},
	],
	password: [
		{
			required: true,
			trigger: 'change',
			validator: (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
				passwordValid.value = false;
				if (!PASSWORD_FORMAT_PATTERN.test(value)) {
					callback(new Error(PASSWORD_FORMAT_MESSAGE));
				} else {
					passwordValid.value = true;
					callback();
				}
			},
		},
	],
	confirmPassword: [
		{
			required: true,
			trigger: 'change',
			validator: (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
				confirmPasswordValid.value = false;
				if (!value) {
					callback(new Error('请再次输入密码。'));
				} else if (value !== registerForm.password) {
					callback(new Error('两次输入的密码不一致。'));
				} else {
					confirmPasswordValid.value = true;
					callback();
				}
			},
		},
	],
	verificationCode: [
		{
			required: true,
			trigger: 'change',
			validator: (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
				verificationCodeValid.value = false;
				if (!EMAIL_VERIFICATION_CODE_PATTERN.test(value)) {
					callback(new Error(EMAIL_VERIFICATION_CODE_MESSAGE));
				} else {
					verificationCodeValid.value = true;
					callback();
				}
			},
		},
	],
};

// 控制注册按钮是否可用
let validated = ref<boolean>(false);

// 监听并更新按钮是否可用
watch(
	() => [
		usernameAvailable.value,
		emailAvailable.value,
		passwordValid.value,
		confirmPasswordValid.value,
		verificationCodeValid.value,
		checked.value,
	],
	() => {
		validated.value =
			usernameAvailable.value &&
			emailAvailable.value &&
			passwordValid.value &&
			confirmPasswordValid.value &&
			verificationCodeValid.value &&
			checked.value;
	},
);

// 密码变化后重新校验确认密码，避免已通过的确认值继续保持有效。
watch(
	() => registerForm.password,
	() => {
		confirmPasswordValid.value = false;
		if (registerForm.confirmPassword) {
			void registerFormRef.value?.validateField('confirmPassword').catch(() => undefined);
		}
	},
);

// 邮箱改变后，旧邮箱收到的验证码不能继续用于当前表单。
watch(
	() => registerForm.email,
	() => {
		registerForm.verificationCode = '';
		verificationCodeValid.value = false;
		remainingSeconds.value = 0;
		pauseCountdown();
	},
);

// 发送注册邮箱验证码，服务端成功接收请求后开始本地倒计时。
async function sendCode() {
	if (!emailAvailable.value || sendingCode.value || remainingSeconds.value > 0) return;
	sendingCode.value = true;
	try {
		await sendEmailVerificationCode({ email: registerForm.email });
		remainingSeconds.value = 60;
		resumeCountdown();
		ElMessage.success('验证码已发送，请查收邮件');
	} catch {
		// 错误提示已经由 request 响应拦截器统一处理
	} finally {
		sendingCode.value = false;
	}
}

// 注册
async function handlerRegister() {
	if (!validated.value) return;

	try {
		if (!privacyPolicyVersion.value) {
			const policy = await getPrivacyPolicy();
			privacyPolicyVersion.value = policy.version;
		}

		await register({
			username: registerForm.username,
			email: registerForm.email,
			password: registerForm.password,
			verificationCode: registerForm.verificationCode,
			privacyPolicyVersion: privacyPolicyVersion.value,
		});
		router.replace('/auth/login');
		ElMessage.success('注册成功，请登录');
	} catch (error) {
		if (
			isAxiosError<ApiResult>(error) &&
			error.response?.data?.code === ApiCode.PRIVACY_POLICY_VERSION_MISMATCH
		) {
			privacyPolicyVersion.value = '';
			ElMessage.warning('隐私政策已更新，请重新打开隐私政策页面');
			privacyPolicyVisible.value = false;
			checked.value = false; // 需要用户手动重新确认
			await nextTick();
			privacyPolicyVisible.value = true;
			return;
		}
		if (
			isAxiosError<ApiResult>(error) &&
			error.response?.data?.code === ApiCode.EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED
		) {
			registerForm.verificationCode = '';
			verificationCodeValid.value = false;
		}
		// 错误提示已经由 request 响应拦截器统一处理
	}
}

function handlePrivacyPolicyLoaded(policy: { version: string }) {
	privacyPolicyVersion.value = policy.version;
}
</script>

<style lang="scss" scoped>
.auth-card {
	--el-font-size-base: 1rem;

	:deep(.el-form-item__error) {
		margin-top: 4px;
		margin-left: 2px;
		line-height: 0.8rem;
	}
}

.auth-footer-checkbox {
	:deep(.el-checkbox__label) {
		font-size: 0.8rem;
	}
}

.auth-footer-link {
	--el-link-font-size: 0.8rem;
}

.verification-code-field {
	display: grid;
	grid-template-columns: minmax(0, 1fr) 8.5rem;
	gap: 0.5rem;
	width: 100%;
}

.verification-code-button {
	width: 8.5rem;
	margin-left: 0;
}

.verification-code-button.is-disabled {
	--el-button-disabled-bg-color: var(--app-surface-muted);
	--el-button-disabled-border-color: var(--app-border);
	--el-button-disabled-text-color: var(--app-text-disabled);

	opacity: 1;
}

.register-form {
	:deep(.el-form-item) {
		margin-bottom: 32px;
	}

	:deep(.el-form-item__label) {
		font-weight: 500;
	}
}
</style>
