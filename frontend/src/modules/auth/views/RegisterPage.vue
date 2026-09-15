<template>
	<el-card class="auth-card">
		<h1 class="mt-2 mb-4 text-center text-2xl">注册</h1>
		<hr class="mb-4 text-gray-300" />
		<el-form
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
					placeholder="请输入用户名"
				>
					<template #prefix>
						<i-ep-user />
					</template>
				</el-input>
			</el-form-item>
			<el-form-item
				prop="email"
				label="邮箱"
			>
				<el-input
					v-model="registerForm.email"
					placeholder="请输入邮箱"
				>
					<template #prefix>
						<i-ep-message />
					</template>
				</el-input>
			</el-form-item>
			<el-form-item
				prop="password"
				label="密码"
			>
				<el-input
					v-model="registerForm.password"
					placeholder="请输入密码"
					type="password"
					show-password
				>
					<template #prefix>
						<i-ep-lock />
					</template>
				</el-input>
			</el-form-item>
			<el-button
				type="primary"
				native-type="submit"
				class="my-6 w-full"
				:disabled="!validated"
			>
				创建账号
			</el-button>
		</el-form>

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
import { ref, reactive, watch } from 'vue';
import { useRouter } from 'vue-router';
import type { RegisterRequest } from '../types/auth';
import { register } from '../api/authApi';
import type { FormItemRule } from 'element-plus';
import { ElMessage } from 'element-plus';
import {
	EMAIL_FORMAT_MESSAGE,
	EMAIL_FORMAT_PATTERN,
	PASSWORD_FORMAT_MESSAGE,
	PASSWORD_FORMAT_PATTERN,
	USERNAME_FORMAT_MESSAGE,
	USERNAME_FORMAT_PATTERN,
} from '@/constants/validation';

defineOptions({
	name: 'RegisterPage',
});

const router = useRouter();

// 注册表单数据
let registerForm = reactive<RegisterRequest>({
	username: '',
	email: '',
	password: '',
});

let usernameAvailable = ref<boolean>(false);
let emailAvailable = ref<boolean>(false);
let passwordValid = ref<boolean>(false);

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
};

// 控制注册按钮是否可用
let validated = ref<boolean>(false);

// 监听并更新按钮是否可用
watch(
	() => [usernameAvailable.value, emailAvailable.value, passwordValid.value],
	() => {
		validated.value = usernameAvailable.value && emailAvailable.value && passwordValid.value;
	},
);

// 注册
async function handlerRegister() {
	if (!validated.value) return;
	try {
		await register(registerForm);
		router.replace('/auth/login');
		ElMessage.success('注册成功，请登录');
	} catch {
		// 错误提示已经由 request 响应拦截器统一处理
	}
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

.auth-footer-link {
	--el-link-font-size: 0.8rem;
}
</style>
